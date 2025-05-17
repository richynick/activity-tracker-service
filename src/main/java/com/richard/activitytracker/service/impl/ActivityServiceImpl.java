package com.richard.activitytracker.service.impl;

import com.richard.activitytracker.dto.ActivityRequest;
import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.exception.BroadcastFailedException;
import com.richard.activitytracker.exception.UserNotFoundException;
import com.richard.activitytracker.exception.WebSocketException;
import com.richard.activitytracker.model.Activity;
import com.richard.activitytracker.model.User;
import com.richard.activitytracker.repository.ActivityRepository;
import com.richard.activitytracker.repository.UserRepository;
import com.richard.activitytracker.service.ActivityService;
import com.richard.activitytracker.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final WebSocketService webSocketService;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

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
            throw new BroadcastFailedException("Activity logged but failed to broadcast", e);
        }
        return response;
    }

    @Override
    @Cacheable(value = "recentActivities", key = "#pageable.pageNumber")
    public Page<ActivityResponse> getRecentActivities(Pageable pageable) {
        return activityRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Cacheable(value = "userActivities", key = "#userId + '-' + #pageable.pageNumber")
    public Page<ActivityResponse> getActivitiesByUserId(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            log.error("User not found: {}", userId);
            throw new UserNotFoundException("User with ID " + userId + " not found");
        }

        return activityRepository.findByUserId(userId, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Cacheable(value = "searchActivities", key = "#userId + '-' + #startTime + '-' + #endTime + '-' + #pageable.pageNumber")
    public Page<ActivityResponse> searchActivities(Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        if (userId != null && !userRepository.existsById(userId)) {
            log.error("User not found: {}", userId);
            throw new UserNotFoundException("User with ID " + userId + " not found");
        }

        if (userId != null) {
            return activityRepository.findByUserIdAndTimestampBetween(userId, startTime, endTime, pageable)
                    .map(this::convertToResponse);
        } else {
            return activityRepository.findByTimestampBetween(startTime, endTime, pageable)
                    .map(this::convertToResponse);
        }
    }

    @Override
    @Transactional
    public ActivityResponse createActivity(Long userId, String action, String details) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found"));

        Activity activity = new Activity();
        activity.setUser(user);
        activity.setAction(action);
        activity.setDetails(details);

        Activity savedActivity = activityRepository.save(activity);
        ActivityResponse response = convertToResponse(savedActivity);

        try {
            webSocketService.broadcastActivity(response);
            log.info("Activity created successfully for user {}: {}", userId, action);
        } catch (WebSocketException e) {
            log.error("Failed to broadcast activity: {}", e.getMessage());
            throw new BroadcastFailedException("Activity created but failed to broadcast", e);
        }
        return response;
    }

    @Override
    @Cacheable(value = "allActivities", key = "#pageable.pageNumber")
    public Page<ActivityResponse> getAllActivities(Pageable pageable) {
        return activityRepository.findAll(pageable)
                .map(this::convertToResponse);
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
} 
