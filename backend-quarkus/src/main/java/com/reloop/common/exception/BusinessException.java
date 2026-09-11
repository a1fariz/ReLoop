package com.reloop.common.exception;

public class BusinessException extends RuntimeException {
    private final int statusCode;
    private final String code;

    public BusinessException(String message, String code, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public BusinessException(String message, String code) {
        this(message, code, 422); // Unprocessable Entity, same default as legacy
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }
}
