package com.daifu.manage.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductUpdateStatusRequest(
        @NotNull(message = "status is required") @Min(value = 0) @Max(value = 1) Integer status
) {
}

