package com.daifu.manage.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatJsapiPayRequest(
        @NotBlank(message = "orderNo is required") String orderNo
) {
}
