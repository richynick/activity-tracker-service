package com.richard.activitytracker.handler;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String message,
        String error,
        int status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime timestamp,
        String path,
        Map<String, String> errors
) {
    public ErrorResponse(String message, String error, int status, String path) {
        this(message, error, status, LocalDateTime.now(), path, null);
    }

    public ErrorResponse(String message, String error, int status, String path, Map<String, String> errors) {
        this(message, error, status, LocalDateTime.now(), path, errors);
    }
}
