package com.richard.activitytracker.service;

import com.richard.activitytracker.dto.ActivityRequest;
import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.dto.AuthResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityService {
    ActivityResponse logActivity(Long userId, ActivityRequest request);
    Page<ActivityResponse> getRecentActivities(Pageable pageable);
    Page<ActivityResponse> getActivitiesByUserId(Long userId, Pageable pageable);
    Page<ActivityResponse> searchActivities(Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    ActivityResponse createActivity(Long userId, String action, String details);
    Page<ActivityResponse> getAllActivities(Pageable pageable);
} 