package com.richard.activitytracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.activitytracker.config.TestConfig;
import com.richard.activitytracker.config.TestSecurityConfig;
import com.richard.activitytracker.dto.ActivityRequest;
import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.exception.UserNotFoundException;
import com.richard.activitytracker.model.Role;
import com.richard.activitytracker.model.User;
import com.richard.activitytracker.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
@Import({TestSecurityConfig.class, TestConfig.class})
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    @Autowired
    private ObjectMapper objectMapper;

    private ActivityRequest activityRequest;
    private ActivityResponse activityResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create and set up the test user with a role
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        activityRequest = new ActivityRequest();
        activityRequest.setAction("TEST_ACTION");
        activityRequest.setDetails("Test details");

        activityResponse = ActivityResponse.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .action("TEST_ACTION")
                .details("Test details")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void logActivity_Success() throws Exception {
        when(activityService.logActivity(eq(1L), any(ActivityRequest.class)))
                .thenReturn(activityResponse);

        mockMvc.perform(post("/api/activities")
                .with(user(testUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.action").value("TEST_ACTION"))
                .andExpect(jsonPath("$.details").value("Test details"));
    }

    @Test
    void getRecentActivities_Success() throws Exception {
        List<ActivityResponse> activities = Arrays.asList(activityResponse);
        Page<ActivityResponse> activityPage = new PageImpl<>(activities);
        Pageable pageable = PageRequest.of(0, 10);

        when(activityService.getRecentActivities(any(Pageable.class)))
                .thenReturn(activityPage);

        mockMvc.perform(get("/api/activities")
                .with(user(testUser))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].action").value("TEST_ACTION"));
    }

    @Test
    void getActivitiesByUserId_Success() throws Exception {
        List<ActivityResponse> activities = Arrays.asList(activityResponse);
        Page<ActivityResponse> activityPage = new PageImpl<>(activities);
        Pageable pageable = PageRequest.of(0, 10);

        when(activityService.getActivitiesByUserId(any(Long.class), any(Pageable.class)))
                .thenReturn(activityPage);

        mockMvc.perform(get("/api/activities/user/1")
                .with(user(testUser))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].action").value("TEST_ACTION"));
    }

    @Test
    void getActivitiesByUserId_UserNotFound() throws Exception {
        when(activityService.getActivitiesByUserId(any(Long.class), any(Pageable.class)))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/activities/user/999")
                .with(user(testUser))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchActivities_Success() throws Exception {
        List<ActivityResponse> activities = Arrays.asList(activityResponse);
        Page<ActivityResponse> activityPage = new PageImpl<>(activities);
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(activityService.searchActivities(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(activityPage);

        mockMvc.perform(get("/api/activities/search")
                .with(user(testUser))
                .param("userId", "1")
                .param("startTime", startTime.toString())
                .param("endTime", endTime.toString())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].action").value("TEST_ACTION"));
    }

    @Test
    void searchActivities_UserNotFound() throws Exception {
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(activityService.searchActivities(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/activities/search")
                .with(user(testUser))
                .param("userId", "999")
                .param("startTime", startTime.toString())
                .param("endTime", endTime.toString())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isNotFound());
    }
} 