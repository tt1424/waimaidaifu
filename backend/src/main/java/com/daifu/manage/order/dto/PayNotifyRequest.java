package com.daifu.manage.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayNotifyRequest(
        @NotBlank(message = "orderNo is required") String orderNo,
        @NotNull(message = "payAmount is required") @DecimalMin(value = "0.01", message = "payAmount must be > 0") BigDecimal payAmount,
        @NotBlank(message = "payTime is required") String payTime
) {
}

