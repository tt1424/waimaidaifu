package com.daifu.manage.payment.vo;

import java.math.BigDecimal;

public record PaymentOrderItemVO(
        Long productId,
        String productName,
        BigDecimal price,
        Integer quantity,
        BigDecimal subTotal
) {
}
