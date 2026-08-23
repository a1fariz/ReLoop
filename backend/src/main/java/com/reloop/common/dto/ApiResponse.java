package com.reloop.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    Instant timestamp,
    String correlationId
) {
    public static <T> ApiResponse<T> ok(T data, String message, String correlationId) {
        return new ApiResponse<>(true, data, message, Instant.now(), correlationId);
    }

    public static <T> ApiResponse<T> ok(T data, String correlationId) {
        return ok(data, "Operation successful", correlationId);
    }
}
