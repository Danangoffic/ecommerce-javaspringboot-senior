package com.apex.ecommerce.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
    int statusCode,
    String error,
    String message,
    LocalDateTime timestamp,
    Map<String, String> details
) {
    public ErrorResponse(int statusCode, String error, String message) {
        this(statusCode, error, message, LocalDateTime.now(), null);
    }
}
