package com.daifu.manage.cart.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemVO(
        Long id,
        Long userId,
        Long productId,
        String productName,
        Long storeId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        LocalDateTime updateTime
) {
}
