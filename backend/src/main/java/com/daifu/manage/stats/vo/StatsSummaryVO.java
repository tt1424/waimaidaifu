package com.daifu.manage.stats.vo;

import java.math.BigDecimal;

public record StatsSummaryVO(
        Integer activeUserCount,
        Integer totalQuantity,
        BigDecimal totalAmount
) {
}
