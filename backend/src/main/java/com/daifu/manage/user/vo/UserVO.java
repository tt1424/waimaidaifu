package com.daifu.manage.user.vo;

import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String username,
        String phone,
        Integer role,
        Integer status,
        LocalDateTime createTime
) {
}
