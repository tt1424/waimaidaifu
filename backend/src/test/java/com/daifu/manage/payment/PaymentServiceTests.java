package com.daifu.manage.payment;

import com.daifu.manage.cart.entity.CartItem;
import com.daifu.manage.cart.mapper.CartItemMapper;
import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.payment.config.WechatPayProperties;
import com.daifu.manage.payment.dto.CheckoutCreateRequest;
import com.daifu.manage.payment.dto.WechatJsapiPayRequest;
import com.daifu.manage.payment.entity.PaymentOrder;
import com.daifu.manage.payment.entity.PaymentOrderItem;
import com.daifu.manage.payment.mapper.PaymentNotifyLogMapper;
import com.daifu.manage.payment.mapper.PaymentOrderItemMapper;
import com.daifu.manage.payment.mapper.PaymentOrderMapper;
import com.daifu.manage.payment.service.PaymentService;
import com.daifu.manage.payment.service.WechatPayClient;
import com.daifu.manage.payment.vo.CheckoutCreateVO;
import com.daifu.manage.payment.vo.PaymentOrderVO;
import com.daifu.manage.payment.vo.WechatJsapiPayVO;
import com.daifu.manage.product.entity.Product;
import com.daifu.manage.product.mapper.ProductMapper;
import com.daifu.manage.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    @Mock
    private PaymentOrderItemMapper paymentOrderItemMapper;

    @Mock
    private PaymentNotifyLogMapper paymentNotifyLogMapper;

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserService userService;

    @Mock
    private WechatPayClient wechatPayClient;

    private final WechatPayProperties wechatPayProperties = new WechatPayProperties();

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentOrderMapper,
                paymentOrderItemMapper,
                paymentNotifyLogMapper,
                cartItemMapper,
                productMapper,
                userService,
                wechatPayProperties,
                wechatPayClient,
                new ObjectMapper()
        );
    }

    @Test
    void createCheckout_allowsMissingOpenidAndReturnsCashierPath() {
        wechatPayProperties.setOrderExpireMinutes(30);
        when(userService.existsEnabledUser(12L)).thenReturn(true);
        when(cartItemMapper.selectBatchIds(List.of(7L))).thenReturn(List.of(cartItem(7L, 12L, 100L, 2)));
        when(productMapper.selectBatchIds(Set.of(100L))).thenReturn(List.of(product(100L, "Latte", "19.90", 20)));
        doAnswer(invocation -> {
            PaymentOrder order = invocation.getArgument(0);
            order.setId(88L);
            return 1;
        }).when(paymentOrderMapper).insert(any(PaymentOrder.class));

        CheckoutCreateVO result = paymentService.createCheckout(new CheckoutCreateRequest(12L, List.of(7L), null));

        ArgumentCaptor<PaymentOrder> orderCaptor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderMapper).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOpenid()).isNull();
        assertThat(result.totalAmount()).isEqualByComparingTo("39.80");
        assertThat(readProperty(result, "cashierPath")).isEqualTo("/cashier/" + result.orderNo());
    }

    @Test
    void wechatJsapiPay_inMockModeBindsGeneratedOpenidAndReturnsMockPayload() {
        ReflectionTestUtils.setField(wechatPayProperties, "mode", "mock");
        wechatPayProperties.setAppId("mock-app");

        PaymentOrder order = new PaymentOrder();
        order.setId(88L);
        order.setOrderNo("ORD-MOCK-1");
        order.setUserId(12L);
        order.setTotalAmount(new BigDecimal("39.80"));
        order.setStatus(0);
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));

        when(paymentOrderMapper.selectOne(any())).thenReturn(order);

        WechatJsapiPayVO result = paymentService.wechatJsapiPay(new WechatJsapiPayRequest("ORD-MOCK-1"));

        assertThat(result.prepayId()).startsWith("mock_prepay_");
        assertThat(order.getOpenid()).startsWith("mock-openid-12");
        verify(paymentOrderMapper).updateById(order);
        verify(wechatPayClient, never()).unifiedOrderJsapi(any(), any(), any());
    }

    @Test
    void getOrder_closesExpiredOrderAndReturnsItems() {
        PaymentOrder order = new PaymentOrder();
        order.setId(66L);
        order.setOrderNo("ORD-EXPIRED-1");
        order.setUserId(10L);
        order.setTotalAmount(new BigDecimal("59.90"));
        order.setStatus(0);
        order.setExpireTime(LocalDateTime.now().minusMinutes(1));

        PaymentOrderItem item = new PaymentOrderItem();
        item.setOrderId(66L);
        item.setProductId(9L);
        item.setProductName("Demo Product");
        item.setPrice(new BigDecimal("59.90"));
        item.setQuantity(1);
        item.setSubTotal(new BigDecimal("59.90"));

        when(paymentOrderMapper.selectOne(any())).thenReturn(order);
        when(paymentOrderItemMapper.selectList(any())).thenReturn(List.of(item));

        PaymentOrderVO result = paymentService.getOrder("ORD-EXPIRED-1");

        assertThat(result.status()).isEqualTo(2);
        verify(paymentOrderMapper).updateById(order);
        assertThat(readProperty(result, "items")).asList().hasSize(1);
    }

    @Test
    void markOrderPaid_updatesOrderStockAndCart() {
        PaymentOrder order = new PaymentOrder();
        order.setId(99L);
        order.setOrderNo("ORD-PAID-1");
        order.setStatus(0);

        PaymentOrderItem item = new PaymentOrderItem();
        item.setOrderId(99L);
        item.setCartItemId(7L);
        item.setProductId(100L);
        item.setQuantity(2);

        when(paymentOrderMapper.selectOne(any())).thenReturn(order);
        when(paymentOrderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(productMapper.decreaseStock(100L, 2)).thenReturn(1);

        new TestablePaymentService().invokeMarkOrderPaid("ORD-PAID-1", "WX-TX-1");

        assertThat(order.getStatus()).isEqualTo(1);
        assertThat(order.getWechatTransactionId()).isEqualTo("WX-TX-1");
        verify(cartItemMapper).deleteBatchIds(Set.of(7L));
        verify(paymentOrderMapper).updateById(order);
    }

    @Test
    void createCheckout_rejectsMissingUser() {
        when(userService.existsEnabledUser(12L)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.createCheckout(new CheckoutCreateRequest(12L, List.of(7L), null)))
                .isInstanceOf(BizException.class)
                .hasMessage("user not found or disabled");
    }

    private Object readProperty(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (Exception ex) {
            throw new AssertionError("missing property: " + name, ex);
        }
    }

    private CartItem cartItem(Long id, Long userId, Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private Product product(Long id, String name, String price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setStatus(1);
        return product;
    }

    private class TestablePaymentService extends PaymentService {

        TestablePaymentService() {
            super(
                    paymentOrderMapper,
                    paymentOrderItemMapper,
                    paymentNotifyLogMapper,
                    cartItemMapper,
                    productMapper,
                    userService,
                    wechatPayProperties,
                    wechatPayClient,
                    new ObjectMapper()
            );
        }

        void invokeMarkOrderPaid(String orderNo, String transactionId) {
            super.markOrderPaid(orderNo, transactionId, LocalDateTime.now());
        }
    }
}
