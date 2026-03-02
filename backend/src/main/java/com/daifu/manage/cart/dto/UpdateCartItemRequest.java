package com.daifu.manage.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull(message = "quantity is required") @Min(value = 1, message = "quantity must be >= 1") Integer quantity
) {
}
