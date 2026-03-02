package com.daifu.manage.cart.vo;

import java.math.BigDecimal;

public record CartSummaryVO(
        Integer itemCount,
        Integer totalQuantity,
        BigDecimal totalAmount
) {
}
