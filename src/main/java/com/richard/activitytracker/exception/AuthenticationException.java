package com.richard.activitytracker.exception;

import java.util.Map;

public class AuthenticationException extends RuntimeException {
    private final String error;
    private final int status;
    private final String path;
    private final Map<String, String> details;

    public AuthenticationException(String message, String error, int status, String path) {
        super(message);
        this.error = error;
        this.status = status;
        this.path = path;
        this.details = null;
    }

    public AuthenticationException(String message, String error, int status, String path, Map<String, String> details) {
        super(message);
        this.error = error;
        this.status = status;
        this.path = path;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public int getStatus() {
        return status;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getDetails() {
        return details;
    }
} 