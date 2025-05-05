package com.richard.activitytracker.controller;

import com.richard.activitytracker.dto.ActivityRequest;
import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.dto.AuthResponse;
import com.richard.activitytracker.model.User;
import com.richard.activitytracker.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {
    private final ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityResponse> logActivity(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(activityService.logActivity(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<ActivityResponse> getRecentActivities() {
        return ResponseEntity.ok(activityService.getRecentActivities());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getActivitiesByUserId(
            @PathVariable Long userId,
            Pageable pageable) {
        return activityService.getActivitiesByUserId(userId, pageable);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchActivities(
            @RequestParam(required = false) Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            Pageable pageable) {
        return activityService.searchActivities(userId, startTime, endTime, pageable);
    }
} 