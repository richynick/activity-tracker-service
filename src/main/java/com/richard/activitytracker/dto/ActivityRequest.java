package com.richard.activitytracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActivityRequest {

    private Long userId;

    @NotBlank(message = "Action is required")
    private String action;
    
    @NotBlank(message = "Details are required")
    private String details;
} 