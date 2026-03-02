package com.daifu.manage.stats.vo;

import java.math.BigDecimal;

public record UserStatsVO(
        Long userId,
        String username,
        Integer totalQuantity,
        BigDecimal totalAmount
) {
}
