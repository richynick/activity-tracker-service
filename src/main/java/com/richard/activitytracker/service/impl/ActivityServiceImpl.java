package com.richard.activitytracker.service.impl;

import com.richard.activitytracker.dto.ActivityRequest;
import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.exception.BroadcastFailedException;
import com.richard.activitytracker.exception.UserNotFoundException;
import com.richard.activitytracker.exception.WebSocketException;
import com.richard.activitytracker.handler.ErrorResponse;
import com.richard.activitytracker.model.Activity;
import com.richard.activitytracker.model.User;
import com.richard.activitytracker.repository.ActivityRepository;
import com.richard.activitytracker.repository.UserRepository;
import com.richard.activitytracker.service.ActivityService;
import com.richard.activitytracker.service.WebSocketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final WebSocketService webSocketService;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    @Lazy
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ActivityResponse logActivity(Long userId, ActivityRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return new UserNotFoundException("User with ID " + userId + " not found");
                });

        Activity activity = new Activity();
        activity.setUser(user);
        activity.setAction(request.getAction());
        activity.setDetails(request.getDetails());

        Activity savedActivity = activityRepository.save(activity);
        ActivityResponse response = convertToResponse(savedActivity);

        try {
            webSocketService.broadcastActivity(response);
            log.info("Activity logged successfully for user {}: {}", userId, response.getAction());
        } catch (WebSocketException e) {
            log.error("Failed to broadcast activity: {}", e.getMessage());
            // Optionally, throw a custom exception or just log the error
            throw new BroadcastFailedException("Activity logged but failed to broadcast", e);
        }
        return response;
    }

    @Override
    @Cacheable(value = "recentActivities", key = "#pageable.pageNumber")
    public ActivityResponse getRecentActivities() {
        try {
            Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<Activity> activities = activityRepository.findAll(pageable);
            if (activities.hasContent()) {
                Activity recentActivity = activities.getContent().get(0);
                ActivityResponse response = convertToResponse(recentActivity);
                log.info("Retrieved most recent activity: {}", response.getId());
                return response;
            } else {
                log.warn("No activities found");
                return null; // Or throw an exception, or handle as you wish
            }
        } catch (Exception e) {
            log.error("Failed to retrieve recent activity: {}", e.getMessage(), e);
            return null; // Or throw an exception, or handle as you wish
        }
    }

    @Override
    @Cacheable(value = "userActivities", key = "#userId + '-' + #pageable.pageNumber")
    public ResponseEntity<?> getActivitiesByUserId(Long userId, Pageable pageable) {
        try {
            if (!userRepository.existsById(userId)) {
                log.error("User not found: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(
                                "User with ID " + userId + " not found",
                                "User not found",
                                HttpStatus.NOT_FOUND.value(),
                                getCurrentRequestPath()
                        ));
            }

            Page<ActivityResponse> activities = activityRepository.findByUserId(userId, pageable)
                    .map(this::convertToResponse);
            log.info("Retrieved {} activities for user {}", activities.getTotalElements(), userId);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            log.error("Failed to retrieve activities for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            "Failed to retrieve user activities",
                            "Internal server error",
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            getCurrentRequestPath()
                    ));
        }
    }

    @Override
    @Cacheable(value = "searchActivities", key = "#userId + '-' + #startTime + '-' + #endTime + '-' + #pageable.pageNumber")
    public ResponseEntity<?> searchActivities(Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        try {
            if (userId != null && !userRepository.existsById(userId)) {
                log.error("User not found: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(
                                "User with ID " + userId + " not found",
                                "User not found",
                                HttpStatus.NOT_FOUND.value(),
                                getCurrentRequestPath()
                        ));
            }

            Page<ActivityResponse> activities;
            if (userId != null) {
                activities = activityRepository.findByUserIdAndTimestampBetween(userId, startTime, endTime, pageable)
                        .map(this::convertToResponse);
            } else {
                activities = activityRepository.findByTimestampBetween(startTime, endTime, pageable)
                        .map(this::convertToResponse);
            }
            
            log.info("Retrieved {} activities for search criteria", activities.getTotalElements());
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            log.error("Failed to search activities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            "Failed to search activities",
                            "Internal server error",
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            getCurrentRequestPath()
                    ));
        }
    }

    @Override
    public ActivityResponse createActivity(ActivityRequest activityRequest) {
        Activity activity = Activity.builder()
                .action(activityRequest.getAction())
                .details(activityRequest.getDetails())
                .build();
        Activity savedActivity = activityRepository.save(activity);
        ActivityResponse response = ActivityResponse.builder()
                .id(savedActivity.getId())
                .action(savedActivity.getAction())
                .details(savedActivity.getDetails())
                .timestamp(savedActivity.getTimestamp())
                .build();
        
        webSocketService.broadcastActivity(response);
        return response;
    }

    @Override
    public List<ActivityResponse> getAllActivities() {
        return activityRepository.findAll().stream()
                .map(activity -> ActivityResponse.builder()
                        .id(activity.getId())
                        .action(activity.getAction())
                        .details(activity.getDetails())
                        .timestamp(activity.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    private ActivityResponse convertToResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setUserId(activity.getUser().getId());
        response.setUsername(activity.getUser().getUsername());
        response.setAction(activity.getAction());
        response.setDetails(activity.getDetails());
        response.setTimestamp(activity.getTimestamp());
        return response;
    }

    private String getCurrentRequestPath() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            return request.getRequestURI();
        } catch (Exception e) {
            log.warn("Could not get current request path: {}", e.getMessage());
            return "/api/activities";
        }
    }

} 