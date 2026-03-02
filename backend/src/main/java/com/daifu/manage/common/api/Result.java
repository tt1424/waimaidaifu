package com.daifu.manage.common.api;

public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data);
    }

    public static Result<Void> ok() {
        return new Result<>(0, "success", null);
    }

    public static Result<Void> fail(String message) {
        return new Result<>(-1, message, null);
    }
}
