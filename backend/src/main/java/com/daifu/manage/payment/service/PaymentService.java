package com.daifu.manage.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daifu.manage.cart.entity.CartItem;
import com.daifu.manage.cart.mapper.CartItemMapper;
import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.payment.config.WechatPayProperties;
import com.daifu.manage.payment.dto.CheckoutCreateRequest;
import com.daifu.manage.payment.dto.WechatJsapiPayRequest;
import com.daifu.manage.payment.entity.PaymentNotifyLog;
import com.daifu.manage.payment.entity.PaymentOrder;
import com.daifu.manage.payment.entity.PaymentOrderItem;
import com.daifu.manage.payment.mapper.PaymentNotifyLogMapper;
import com.daifu.manage.payment.mapper.PaymentOrderItemMapper;
import com.daifu.manage.payment.mapper.PaymentOrderMapper;
import com.daifu.manage.payment.vo.CheckoutCreateVO;
import com.daifu.manage.payment.vo.PaymentOrderItemVO;
import com.daifu.manage.payment.vo.PaymentOrderVO;
import com.daifu.manage.payment.vo.WechatJsapiPayVO;
import com.daifu.manage.product.entity.Product;
import com.daifu.manage.product.mapper.ProductMapper;
import com.daifu.manage.user.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final int STATUS_UNPAID = 0;
    private static final int STATUS_PAID = 1;
    private static final int STATUS_CLOSED = 2;
    private static final int STATUS_FAILED = 3;

    private static final String CHANNEL_CASHIER = "CASHIER";
    private static final String CHANNEL_WECHAT_JSAPI = "WECHAT_JSAPI";
    private static final String CHANNEL_MOCK_JSAPI = "MOCK_JSAPI";

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentOrderItemMapper paymentOrderItemMapper;
    private final PaymentNotifyLogMapper paymentNotifyLogMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final UserService userService;
    private final WechatPayProperties wechatPayProperties;
    private final WechatPayClient wechatPayClient;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentOrderMapper paymentOrderMapper,
                          PaymentOrderItemMapper paymentOrderItemMapper,
                          PaymentNotifyLogMapper paymentNotifyLogMapper,
                          CartItemMapper cartItemMapper,
                          ProductMapper productMapper,
                          UserService userService,
                          WechatPayProperties wechatPayProperties,
                          WechatPayClient wechatPayClient,
                          ObjectMapper objectMapper) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentOrderItemMapper = paymentOrderItemMapper;
        this.paymentNotifyLogMapper = paymentNotifyLogMapper;
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.userService = userService;
        this.wechatPayProperties = wechatPayProperties;
        this.wechatPayClient = wechatPayClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public CheckoutCreateVO createCheckout(CheckoutCreateRequest request) {
        if (!userService.existsEnabledUser(request.userId())) {
            throw new BizException("user not found or disabled");
        }

        List<Long> selectedIds = request.cartItemIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (selectedIds.isEmpty()) {
            throw new BizException("cartItemIds is empty");
        }

        List<CartItem> cartItems = cartItemMapper.selectBatchIds(selectedIds);
        if (cartItems.size() != selectedIds.size()) {
            throw new BizException("cart item not found");
        }
        if (cartItems.stream().anyMatch(item -> !Objects.equals(item.getUserId(), request.userId()))) {
            throw new BizException("cart item does not belong to user");
        }

        Set<Long> productIds = cartItems.stream().map(CartItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new BizException("product not available: " + cartItem.getProductId());
            }
            if (product.getStock() == null || product.getStock() < cartItem.getQuantity()) {
                throw new BizException("insufficient stock for product: " + product.getName());
            }
            BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subTotal);
        }

        String orderNo = "WX" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(wechatPayProperties.getOrderExpireMinutes());

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNo);
        order.setUserId(request.userId());
        order.setOpenid(trimToNull(request.openid()));
        order.setTotalAmount(totalAmount);
        order.setStatus(STATUS_UNPAID);
        order.setChannel(CHANNEL_CASHIER);
        order.setExpireTime(expireTime);
        paymentOrderMapper.insert(order);

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            PaymentOrderItem item = new PaymentOrderItem();
            item.setOrderId(order.getId());
            item.setCartItemId(cartItem.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setPrice(product.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setSubTotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            paymentOrderItemMapper.insert(item);
        }

        return new CheckoutCreateVO(orderNo, totalAmount, STATUS_UNPAID, expireTime, buildCashierPath(orderNo));
    }

    @Transactional(rollbackFor = Exception.class)
    public WechatJsapiPayVO wechatJsapiPay(WechatJsapiPayRequest request) {
        PaymentOrder order = findOrderByNo(request.orderNo());
        closeExpiredIfNeeded(order);
        assertPayable(order);

        String openid = resolveOpenid(order);
        if (isMockMode()) {
            return buildMockJsapiPay(order, openid);
        }

        order.setOpenid(openid);
        String prepayId = wechatPayClient.unifiedOrderJsapi(order.getOrderNo(), order.getTotalAmount(), openid);
        order.setChannel(CHANNEL_WECHAT_JSAPI);
        order.setPrepayId(prepayId);
        paymentOrderMapper.updateById(order);
        return wechatPayClient.buildJsapiPayParams(order.getOrderNo(), prepayId);
    }

    public PaymentOrderVO getOrder(String orderNo) {
        PaymentOrder order = findOrderByNo(orderNo);
        closeExpiredIfNeeded(order);
        return toVO(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void simulatePaySuccess(String orderNo) {
        if (!isMockMode()) {
            throw new BizException("mock pay is disabled in real mode");
        }
        PaymentOrder order = findOrderByNo(orderNo);
        closeExpiredIfNeeded(order);
        assertPayable(order);
        markOrderPaid(orderNo, "MOCK-" + UUID.randomUUID().toString().replace("-", ""), LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean handleWechatNotify(String body, Map<String, String> headers) {
        if (isMockMode()) {
            return false;
        }

        JsonNode root = readJson(body);
        String notifyId = root.path("id").asText("");
        if (StringUtils.hasText(notifyId) && notifyExists(notifyId)) {
            return true;
        }

        String timestamp = header(headers, "Wechatpay-Timestamp");
        String nonce = header(headers, "Wechatpay-Nonce");
        String signature = header(headers, "Wechatpay-Signature");
        String eventType = root.path("event_type").asText("");

        boolean verified = wechatPayClient.verifySignature(timestamp, nonce, body, signature);
        PaymentNotifyLog notifyLog = new PaymentNotifyLog();
        notifyLog.setNotifyId(trimToNull(notifyId));
        notifyLog.setEventType(trimToNull(eventType));
        notifyLog.setVerifyStatus(verified ? 1 : 0);
        notifyLog.setRawData(body);
        notifyLog.setOrderNo("");

        if (!verified) {
            paymentNotifyLogMapper.insert(notifyLog);
            return false;
        }

        JsonNode resource = root.path("resource");
        String associatedData = resource.path("associated_data").asText("");
        String cipherNonce = resource.path("nonce").asText("");
        String ciphertext = resource.path("ciphertext").asText("");
        String decrypted = wechatPayClient.decryptNotifyResource(associatedData, cipherNonce, ciphertext);
        JsonNode payNode = readJson(decrypted);

        String orderNo = payNode.path("out_trade_no").asText("");
        String transactionId = payNode.path("transaction_id").asText("");
        String tradeState = payNode.path("trade_state").asText("");
        String successTime = payNode.path("success_time").asText("");

        PaymentOrder order = findOrderByNo(orderNo);
        validateNotifyPayload(order, payNode);

        notifyLog.setOrderNo(orderNo);
        paymentNotifyLogMapper.insert(notifyLog);

        if ("SUCCESS".equalsIgnoreCase(tradeState)) {
            markOrderPaid(orderNo, transactionId, parsePayTime(successTime));
            return true;
        }

        markOrderFailed(orderNo);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    protected void markOrderPaid(String orderNo, String transactionId, LocalDateTime payTime) {
        PaymentOrder order = paymentOrderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null) {
            order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getOrderNo, orderNo)
                    .last("limit 1"));
        }
        if (order == null) {
            return;
        }
        if (Objects.equals(order.getStatus(), STATUS_PAID)) {
            return;
        }
        if (Objects.equals(order.getStatus(), STATUS_CLOSED)) {
            throw new BizException("order already closed");
        }

        List<PaymentOrderItem> items = paymentOrderItemMapper.selectList(new LambdaQueryWrapper<PaymentOrderItem>()
                .eq(PaymentOrderItem::getOrderId, order.getId()));

        for (PaymentOrderItem item : items) {
            int changed = productMapper.decreaseStock(item.getProductId(), item.getQuantity());
            if (changed <= 0) {
                throw new BizException("insufficient stock when pay callback");
            }
        }

        List<Long> cartItemIds = items.stream()
                .map(PaymentOrderItem::getCartItemId)
                .filter(Objects::nonNull)
                .toList();
        if (!cartItemIds.isEmpty()) {
            cartItemMapper.deleteBatchIds(new HashSet<>(cartItemIds));
        }

        order.setStatus(STATUS_PAID);
        order.setWechatTransactionId(transactionId);
        order.setPayTime(payTime == null ? LocalDateTime.now() : payTime);
        paymentOrderMapper.updateById(order);
    }

    private void markOrderFailed(String orderNo) {
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderNo, orderNo)
                .last("limit 1"));
        if (order == null || order.getStatus() == null || order.getStatus() != STATUS_UNPAID) {
            return;
        }
        order.setStatus(STATUS_FAILED);
        paymentOrderMapper.updateById(order);
    }

    private PaymentOrder findOrderByNo(String orderNo) {
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderNo, orderNo)
                .last("limit 1"));
        if (order == null) {
            throw new BizException("payment order not found");
        }
        return order;
    }

    private PaymentOrderVO toVO(PaymentOrder order) {
        List<PaymentOrderItemVO> items = paymentOrderItemMapper.selectList(new LambdaQueryWrapper<PaymentOrderItem>()
                        .eq(PaymentOrderItem::getOrderId, order.getId())
                        .orderByAsc(PaymentOrderItem::getId))
                .stream()
                .map(item -> new PaymentOrderItemVO(
                        item.getProductId(),
                        item.getProductName(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getSubTotal()
                ))
                .toList();

        return new PaymentOrderVO(
                order.getOrderNo(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPrepayId(),
                order.getWechatTransactionId(),
                order.getExpireTime(),
                order.getPayTime(),
                buildCashierPath(order.getOrderNo()),
                items,
                isMockMode()
        );
    }

    private void validateNotifyPayload(PaymentOrder order, JsonNode payNode) {
        int expectedFen = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValue();
        int actualFen = payNode.path("amount").path("total").asInt(-1);
        if (actualFen != expectedFen) {
            throw new BizException("wechat notify amount mismatch");
        }

        String appId = payNode.path("appid").asText("");
        if (StringUtils.hasText(wechatPayProperties.getAppId()) && StringUtils.hasText(appId)
                && !Objects.equals(wechatPayProperties.getAppId(), appId)) {
            throw new BizException("wechat notify appid mismatch");
        }

        String mchId = payNode.path("mchid").asText("");
        if (StringUtils.hasText(wechatPayProperties.getMchId()) && StringUtils.hasText(mchId)
                && !Objects.equals(wechatPayProperties.getMchId(), mchId)) {
            throw new BizException("wechat notify mchid mismatch");
        }
    }

    private void closeExpiredIfNeeded(PaymentOrder order) {
        if (!Objects.equals(order.getStatus(), STATUS_UNPAID)) {
            return;
        }
        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            order.setStatus(STATUS_CLOSED);
            paymentOrderMapper.updateById(order);
        }
    }

    private void assertPayable(PaymentOrder order) {
        if (Objects.equals(order.getStatus(), STATUS_PAID)) {
            throw new BizException("order already paid");
        }
        if (Objects.equals(order.getStatus(), STATUS_CLOSED)) {
            throw new BizException("order already closed");
        }
    }

    private String resolveOpenid(PaymentOrder order) {
        String openid = trimToNull(order.getOpenid());
        if (StringUtils.hasText(openid)) {
            return openid;
        }
        if (isMockMode()) {
            return "mock-openid-" + order.getUserId() + "-" + order.getOrderNo().substring(Math.max(0, order.getOrderNo().length() - 6));
        }
        throw new BizException("order openid is empty");
    }

    private WechatJsapiPayVO buildMockJsapiPay(PaymentOrder order, String openid) {
        String prepayId = "mock_prepay_" + UUID.randomUUID().toString().replace("-", "");
        String appId = StringUtils.hasText(wechatPayProperties.getAppId()) ? wechatPayProperties.getAppId() : "mock-app";
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String packageValue = "prepay_id=" + prepayId;

        order.setOpenid(openid);
        order.setChannel(CHANNEL_MOCK_JSAPI);
        order.setPrepayId(prepayId);
        paymentOrderMapper.updateById(order);

        return new WechatJsapiPayVO(
                order.getOrderNo(),
                appId,
                timeStamp,
                nonceStr,
                packageValue,
                "RSA",
                "mock-sign",
                prepayId
        );
    }

    private boolean notifyExists(String notifyId) {
        Long count = paymentNotifyLogMapper.selectCount(new LambdaQueryWrapper<PaymentNotifyLog>()
                .eq(PaymentNotifyLog::getNotifyId, notifyId));
        return count != null && count > 0;
    }

    private boolean isMockMode() {
        return !"real".equalsIgnoreCase(trimToNull(wechatPayProperties.getMode()));
    }

    private String buildCashierPath(String orderNo) {
        return "/cashier/" + orderNo;
    }

    private LocalDateTime parsePayTime(String text) {
        if (!StringUtils.hasText(text)) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            return LocalDateTime.now();
        }
    }

    private String header(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    private JsonNode readJson(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (Exception ex) {
            throw new BizException("invalid json: " + ex.getMessage());
        }
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }
}
