package com.reloop.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    String correlationId,
    List<String> details
) {
}
