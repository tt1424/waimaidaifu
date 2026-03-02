package com.daifu.manage.store.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StoreUpdateRequest(
        @NotBlank(message = "name is required") String name,
        String contactName,
        @Pattern(regexp = "^$|^1\\d{10}$", message = "invalid contactPhone format") String contactPhone,
        @Min(value = 0, message = "status must be 0 or 1") @Max(value = 1, message = "status must be 0 or 1") Integer status
) {
}
