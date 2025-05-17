package com.richard.activitytracker.service;

import com.richard.activitytracker.dto.ActivityRequest;
import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.exception.UserNotFoundException;
import com.richard.activitytracker.model.Activity;
import com.richard.activitytracker.model.User;
import com.richard.activitytracker.repository.ActivityRepository;
import com.richard.activitytracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ActivityResponse logActivity(Long userId, ActivityRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Activity activity = Activity.builder()
                .user(user)
                .action(request.getAction())
                .details(request.getDetails())
                .timestamp(LocalDateTime.now())
                .build();

        Activity savedActivity = activityRepository.save(activity);
        return mapToResponse(savedActivity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getRecentActivities(Pageable pageable) {
        return activityRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getActivitiesByUserId(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        return activityRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> searchActivities(Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        if (userId != null && !userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        if (userId != null) {
            return activityRepository.findByUserIdAndTimestampBetween(userId, startTime, endTime, pageable)
                    .map(this::mapToResponse);
        } else {
            return activityRepository.findByTimestampBetween(startTime, endTime, pageable)
                    .map(this::mapToResponse);
        }
    }

    @Override
    @Transactional
    public ActivityResponse createActivity(Long userId, String action, String details) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Activity activity = Activity.builder()
                .user(user)
                .action(action)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        Activity savedActivity = activityRepository.save(activity);
        return mapToResponse(savedActivity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getAllActivities(Pageable pageable) {
        return activityRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    private ActivityResponse mapToResponse(Activity activity) {
        return ActivityResponse.builder()
                .id(activity.getId())
                .userId(activity.getUser().getId())
                .username(activity.getUser().getUsername())
                .action(activity.getAction())
                .details(activity.getDetails())
                .timestamp(activity.getTimestamp())
                .build();
    }
} 