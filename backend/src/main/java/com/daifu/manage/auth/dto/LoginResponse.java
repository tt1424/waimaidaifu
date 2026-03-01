package com.daifu.manage.auth.dto;

public record LoginResponse(String token, Long expireInSeconds) {
}

