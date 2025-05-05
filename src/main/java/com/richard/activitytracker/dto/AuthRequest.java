package com.richard.activitytracker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AuthRequest {

    @NotEmpty(message = "Username is mandatory")
    @NotNull(message = "Username is mandatory")
    private String username;

    @NotEmpty(message = "Password is mandatory")
    @NotNull(message = "Password is mandatory")
    @Size(min = 8, message = "Password should be 8 characters long minimum")
    private String password;
} 