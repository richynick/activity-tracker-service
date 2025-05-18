package com.richard.activitytracker.service;

import com.richard.activitytracker.dto.ActivityRequest;
import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.exception.BroadcastFailedException;
import com.richard.activitytracker.exception.UserNotFoundException;
import com.richard.activitytracker.exception.WebSocketException;
import com.richard.activitytracker.model.Activity;
import com.richard.activitytracker.model.User;
import com.richard.activitytracker.repository.ActivityRepository;
import com.richard.activitytracker.repository.UserRepository;
import com.richard.activitytracker.service.impl.ActivityServiceImpl;
import com.richard.activitytracker.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private ActivityServiceImpl activityService;

    private User testUser;
    private Activity testActivity;
    private ActivityRequest testActivityRequest;
    private ActivityResponse testActivityResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testActivity = new Activity();
        testActivity.setId(1L);
        testActivity.setUser(testUser);
        testActivity.setAction("TEST_ACTION");
        testActivity.setDetails("Test details");
        testActivity.setTimestamp(LocalDateTime.now());

        testActivityRequest = new ActivityRequest();
        testActivityRequest.setUserId(1L);
        testActivityRequest.setAction("TEST_ACTION");
        testActivityRequest.setDetails("Test details");

        testActivityResponse = ActivityResponse.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .action("TEST_ACTION")
                .details("Test details")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void logActivity_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);
        doNothing().when(webSocketService).broadcastActivity(any(ActivityResponse.class));

        ActivityResponse response = activityService.logActivity(1L, testActivityRequest);

        assertNotNull(response);
        assertEquals(testActivityRequest.getAction(), response.getAction());
        assertEquals(testActivityRequest.getDetails(), response.getDetails());
        verify(activityRepository).save(any(Activity.class));
        verify(webSocketService).broadcastActivity(any(ActivityResponse.class));
    }

    @Test
    void logActivity_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> 
            activityService.logActivity(1L, testActivityRequest)
        );
    }

    @Test
    void logActivity_BroadcastFailed() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);
        doThrow(new WebSocketException("Broadcast failed", "BROADCAST_ERROR")).when(webSocketService).broadcastActivity(any(ActivityResponse.class));

        assertThrows(BroadcastFailedException.class, () -> 
            activityService.logActivity(1L, testActivityRequest)
        );
    }

    @Test
    void getRecentActivities_Success() {
        List<Activity> activities = Arrays.asList(testActivity);
        Page<Activity> activityPage = new PageImpl<>(activities);
        Pageable pageable = PageRequest.of(0, 10);

        when(activityRepository.findAll(any(Pageable.class))).thenReturn(activityPage);

        Page<ActivityResponse> response = activityService.getRecentActivities(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(activityRepository).findAll(any(Pageable.class));
    }

    @Test
    void getActivitiesByUserId_Success() {
        List<Activity> activities = Arrays.asList(testActivity);
        Page<Activity> activityPage = new PageImpl<>(activities);
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(activityRepository.findByUserId(1L, pageable)).thenReturn(activityPage);

        Page<ActivityResponse> response = activityService.getActivitiesByUserId(1L, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(activityRepository).findByUserId(1L, pageable);
    }

    @Test
    void getActivitiesByUserId_UserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> 
            activityService.getActivitiesByUserId(1L, pageable)
        );
    }

    @Test
    void searchActivities_Success() {
        List<Activity> activities = Arrays.asList(testActivity);
        Page<Activity> activityPage = new PageImpl<>(activities);
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(activityRepository.findByUserIdAndTimestampBetween(1L, startTime, endTime, pageable))
                .thenReturn(activityPage);

        Page<ActivityResponse> response = activityService.searchActivities(1L, startTime, endTime, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(activityRepository).findByUserIdAndTimestampBetween(1L, startTime, endTime, pageable);
    }

    @Test
    void searchActivities_UserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> 
            activityService.searchActivities(1L, startTime, endTime, pageable)
        );
    }
} 
