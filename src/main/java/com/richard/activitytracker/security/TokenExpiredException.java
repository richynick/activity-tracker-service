package com.richard.activitytracker.security;

import lombok.Getter;

import java.util.Date;

@Getter
public class TokenExpiredException extends RuntimeException {
    private final Date expirationDate;
    private final Date currentDate;
    private final long expirationDuration;

    public TokenExpiredException(String message, Date expirationDate, Date currentDate, long expirationDuration) {
        super(message);
        this.expirationDate = expirationDate;
        this.currentDate = currentDate;
        this.expirationDuration = expirationDuration;
    }

    public long getTimeUntilExpiration() {
        return expirationDate.getTime() - currentDate.getTime();
    }

    public boolean isExpired() {
        return currentDate.after(expirationDate);
    }
} 