package com.richard.activitytracker.exception;

import lombok.Getter;

@Getter
public class WebSocketException extends RuntimeException {
    private final String code;
    private final String details;

    public WebSocketException(String message, String code, String details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public WebSocketException(String message, String code) {
        this(message, code, null);
    }
} 