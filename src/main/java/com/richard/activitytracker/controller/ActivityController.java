package com.richard.activitytracker.controller;

import com.richard.activitytracker.dto.ActivityRequest;
import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.dto.AuthResponse;
import com.richard.activitytracker.exception.UserNotFoundException;
import com.richard.activitytracker.model.User;
import com.richard.activitytracker.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<Page<ActivityResponse>> getRecentActivities(@RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<ActivityResponse> activities = activityService.getRecentActivities(pageable);
        return activities.isEmpty() ?
                ResponseEntity.noContent().build() :
                ResponseEntity.ok(activities);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ActivityResponse>> getActivitiesByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        try {
            Page<ActivityResponse> activities = activityService.getActivitiesByUserId(userId, pageable);
            return ResponseEntity.ok(activities);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            // Optional: log and return generic server error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ActivityResponse>> searchActivities(
            @RequestParam(required = false) Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        try {
            Page<ActivityResponse> results = activityService.searchActivities(userId, startTime, endTime, pageable);
            return ResponseEntity.ok(results);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 