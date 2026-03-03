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
import java.util.ArrayList;
import java.util.HashMap;
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
        if (!StringUtils.hasText(request.openid())) {
            throw new BizException("openid is required");
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
        order.setOpenid(request.openid().trim());
        order.setTotalAmount(totalAmount);
        order.setStatus(STATUS_UNPAID);
        order.setChannel("WECHAT_JSAPI");
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

        return new CheckoutCreateVO(orderNo, totalAmount, STATUS_UNPAID, expireTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public WechatJsapiPayVO wechatJsapiPay(WechatJsapiPayRequest request) {
        PaymentOrder order = findOrderByNo(request.orderNo());
        if (order.getStatus() != null && order.getStatus() == STATUS_PAID) {
            throw new BizException("order already paid");
        }
        if (order.getStatus() != null && order.getStatus() == STATUS_CLOSED) {
            throw new BizException("order already closed");
        }
        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            order.setStatus(STATUS_CLOSED);
            paymentOrderMapper.updateById(order);
            throw new BizException("order expired");
        }
        if (!StringUtils.hasText(order.getOpenid())) {
            throw new BizException("order openid is empty");
        }

        String prepayId = wechatPayClient.unifiedOrderJsapi(order.getOrderNo(), order.getTotalAmount(), order.getOpenid());
        order.setPrepayId(prepayId);
        paymentOrderMapper.updateById(order);
        return wechatPayClient.buildJsapiPayParams(order.getOrderNo(), prepayId);
    }

    public PaymentOrderVO getOrder(String orderNo) {
        PaymentOrder order = findOrderByNo(orderNo);
        return toVO(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean handleWechatNotify(String body, Map<String, String> headers) {
        String timestamp = header(headers, "Wechatpay-Timestamp");
        String nonce = header(headers, "Wechatpay-Nonce");
        String signature = header(headers, "Wechatpay-Signature");

        JsonNode root = readJson(body);
        String notifyId = root.path("id").asText("");
        String eventType = root.path("event_type").asText("");

        boolean verified = wechatPayClient.verifySignature(timestamp, nonce, body, signature);
        PaymentNotifyLog notifyLog = new PaymentNotifyLog();
        notifyLog.setNotifyId(notifyId);
        notifyLog.setEventType(eventType);
        notifyLog.setVerifyStatus(verified ? 1 : 0);
        notifyLog.setRawData(body);

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
        PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderNo, orderNo)
                .last("limit 1"));
        if (order == null) {
            return;
        }
        if (order.getStatus() != null && order.getStatus() == STATUS_PAID) {
            return;
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
        return new PaymentOrderVO(
                order.getOrderNo(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPrepayId(),
                order.getWechatTransactionId(),
                order.getExpireTime(),
                order.getPayTime()
        );
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
}
