package com.daifu.manage.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentOrderVO(
        String orderNo,
        Long userId,
        BigDecimal totalAmount,
        Integer status,
        String prepayId,
        String wechatTransactionId,
        LocalDateTime expireTime,
        LocalDateTime payTime
) {
}
