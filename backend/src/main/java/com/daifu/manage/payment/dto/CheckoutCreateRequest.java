package com.daifu.manage.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CheckoutCreateRequest(
        @NotNull(message = "userId is required") Long userId,
        @NotEmpty(message = "cartItemIds is required") List<Long> cartItemIds,
        @NotBlank(message = "openid is required") String openid
) {
}
