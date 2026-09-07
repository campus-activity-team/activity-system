package com.example.activity.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
