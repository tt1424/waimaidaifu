package com.daifu.manage.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CheckoutCreateVO(
        String orderNo,
        BigDecimal totalAmount,
        Integer status,
        LocalDateTime expireTime
) {
}
