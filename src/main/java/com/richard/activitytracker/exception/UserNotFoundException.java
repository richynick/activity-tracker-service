package com.richard.activitytracker.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserNotFoundException extends RuntimeException{

    private String message;

    public UserNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    public String getMsg() {
        return message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
