package com.daifu.manage.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserCreateRequest(
        @NotBlank(message = "username is required") String username,
        @NotBlank(message = "password is required") String password,
        @Pattern(regexp = "^$|^1\\d{10}$", message = "invalid phone format") String phone,
        @Min(value = 1, message = "role must be 1 or 2") @Max(value = 2, message = "role must be 1 or 2") Integer role,
        @Min(value = 0, message = "status must be 0 or 1") @Max(value = 1, message = "status must be 0 or 1") Integer status
) {
}
