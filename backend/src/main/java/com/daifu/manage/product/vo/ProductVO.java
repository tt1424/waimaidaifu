package com.daifu.manage.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductVO(
        Long id,
        Long storeId,
        String storeName,
        String name,
        BigDecimal price,
        Integer stock,
        String description,
        Integer status,
        LocalDateTime createTime
) {
}
