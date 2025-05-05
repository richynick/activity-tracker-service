package com.richard.activitytracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String details;
    private LocalDateTime timestamp;
} 