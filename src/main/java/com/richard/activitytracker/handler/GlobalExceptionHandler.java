package com.richard.activitytracker.handler;

import com.richard.activitytracker.exception.BroadcastFailedException;
import com.richard.activitytracker.exception.TokenGenerationException;
import com.richard.activitytracker.exception.UserNotFoundException;
import com.richard.activitytracker.exception.WebSocketException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(UserNotFoundException exp, HttpServletRequest request) {
        log.error("User not found: {}", exp.getMessage());
        return ResponseEntity
                .status(NOT_FOUND)
                .body(new ErrorResponse(
                        exp.getMessage(),
                        "User not found",
                        NOT_FOUND.value(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(EntityNotFoundException exp, HttpServletRequest request) {
        log.error("Entity not found: {}", exp.getMessage());
        return ResponseEntity
                .status(NOT_FOUND)
                .body(new ErrorResponse(
                        exp.getMessage(),
                        "Resource not found",
                        NOT_FOUND.value(),
                        request.getRequestURI()
                ));
    }
    @ExceptionHandler(BroadcastFailedException.class)
    public ResponseEntity<ErrorResponse> handle(BroadcastFailedException exp, HttpServletRequest request) {
        log.error("Entity not found: {}", exp.getMessage());
        return ResponseEntity
                .status(NOT_FOUND)
                .body(new ErrorResponse(
                        exp.getMessage(),
                        "Resource not found",
                        NOT_FOUND.value(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException exp, HttpServletRequest request) {
        log.error("Validation error: {}", exp.getMessage());
        var errors = new HashMap<String, String>();
        exp.getBindingResult().getAllErrors()
                .forEach(error -> {
                    var fieldName = ((FieldError) error).getField();
                    var errorMessage = error.getDefaultMessage();
                    errors.put(fieldName, errorMessage);
                });

        return ResponseEntity
                .status(BAD_REQUEST)
                .body(new ErrorResponse(
                        "Validation failed",
                        "Invalid request data",
                        BAD_REQUEST.value(),
                        request.getRequestURI(),
                        errors
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handle(BadCredentialsException exp, HttpServletRequest request) {
        log.error("Authentication failed: {}", exp.getMessage());
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(new ErrorResponse(
                        "Invalid username or password",
                        "Authentication failed",
                        UNAUTHORIZED.value(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handle(AccessDeniedException exp, HttpServletRequest request) {
        log.error("Access denied: {}", exp.getMessage());
        return ResponseEntity
                .status(FORBIDDEN)
                .body(new ErrorResponse(
                        "You don't have permission to access this resource",
                        "Access denied",
                        FORBIDDEN.value(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handle(NoHandlerFoundException exp, HttpServletRequest request) {
        log.error("Resource not found: {}", exp.getMessage());
        return ResponseEntity
                .status(NOT_FOUND)
                .body(new ErrorResponse(
                        "The requested resource was not found",
                        "Not found",
                        NOT_FOUND.value(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(WebSocketException.class)
    public ResponseEntity<ErrorResponse> handle(WebSocketException exp, HttpServletRequest request) {
        log.error("WebSocket error [{}]: {}", exp.getCode(), exp.getMessage());
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(new ErrorResponse(
                        exp.getMessage(),
                        exp.getCode(),
                        BAD_REQUEST.value(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(UsernameNotFoundException exp, HttpServletRequest request) {
        log.error("User not found during authentication: {}", exp.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                "Invalid username or password",
                "Authentication failed",
                UNAUTHORIZED.value(),
                request.getRequestURI()
        );
        log.debug("Returning error response: {}", errorResponse);
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(errorResponse);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handle(ExpiredJwtException exp, HttpServletRequest request) {
        log.error("JWT token expired: {}", exp.getMessage());
        
        Map<String, String> details = new HashMap<>();
        details.put("expiredAt", exp.getClaims().getExpiration().toString());
        details.put("currentTime", exp.getMessage().split("Current time: ")[1].split(",")[0]);
        details.put("difference", exp.getMessage().split("difference of ")[1].split(" milliseconds")[0] + " milliseconds");
        
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(new ErrorResponse(
                        "Your session has expired. Please log in again.",
                        "Token expired",
                        UNAUTHORIZED.value(),
                        request.getRequestURI(),
                        details
                ));
    }

    @ExceptionHandler(TokenGenerationException.class)
    public ResponseEntity<ErrorResponse> handle(TokenGenerationException exp, HttpServletRequest request) {
        log.error("Token generation failed: {}", exp.getMessage(), exp);
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "Failed to generate authentication token. Please try again.",
                        "Token generation failed",
                        INTERNAL_SERVER_ERROR.value(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception exp, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", exp.getMessage(), exp);
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "An unexpected error occurred",
                        "Internal server error",
                        INTERNAL_SERVER_ERROR.value(),
                        request.getRequestURI()
                ));
    }
}
