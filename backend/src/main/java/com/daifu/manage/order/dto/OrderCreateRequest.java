package com.daifu.manage.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderCreateRequest(
        Long productId,
        @NotBlank(message = "productName is required") String productName,
        @NotNull(message = "amount is required") @DecimalMin(value = "0.01", message = "amount must be > 0") BigDecimal amount,
        @NotNull(message = "expireMinutes is required") Integer expireMinutes
) {
}
