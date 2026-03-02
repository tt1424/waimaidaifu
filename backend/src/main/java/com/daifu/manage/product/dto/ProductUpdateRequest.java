package com.daifu.manage.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotNull(message = "storeId is required") Long storeId,
        @NotBlank(message = "name is required") String name,
        @NotNull(message = "price is required") @DecimalMin(value = "0.01", message = "price must be > 0") BigDecimal price,
        @NotNull(message = "stock is required") @PositiveOrZero(message = "stock must be >= 0") Integer stock,
        String description
) {
}
