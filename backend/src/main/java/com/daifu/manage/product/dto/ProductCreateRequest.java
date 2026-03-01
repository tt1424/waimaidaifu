package com.daifu.manage.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank(message = "name is required") String name,
        @NotNull(message = "price is required") @DecimalMin(value = "0.01", message = "price must be > 0") BigDecimal price
) {
}

