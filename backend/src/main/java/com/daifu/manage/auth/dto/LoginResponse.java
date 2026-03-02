package com.daifu.manage.auth.dto;

public record LoginResponse(String token, Long expireInSeconds, Long userId, String username, Integer role) {
}

