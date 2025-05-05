package com.richard.activitytracker.exception;

public class BroadcastFailedException extends RuntimeException{

    public BroadcastFailedException(String message) {
        super(message);
    }

    public BroadcastFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
