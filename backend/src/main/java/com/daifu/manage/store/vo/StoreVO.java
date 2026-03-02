package com.daifu.manage.store.vo;

import java.time.LocalDateTime;

public record StoreVO(
        Long id,
        String name,
        String contactName,
        String contactPhone,
        Integer status,
        LocalDateTime createTime
) {
}
