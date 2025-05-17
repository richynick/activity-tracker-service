package com.richard.activitytracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.activitytracker.config.TestSecurityConfig;
import com.richard.activitytracker.dto.ActivityRequest;
import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.exception.UserNotFoundException;
import com.richard.activitytracker.model.Role;
import com.richard.activitytracker.model.User;
import com.richard.activitytracker.security.JwtService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
@Import(TestSecurityConfig.class)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private ActivityRequest activityRequest;
    private ActivityResponse activityResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.USER)
                .build();

        activityRequest = new ActivityRequest();
        activityRequest.setAction("TEST_ACTION");
        activityRequest.setDetails("Test activity details");

        activityResponse = ActivityResponse.builder()
                .id(1L)
                .userId(testUser.getId())
                .username(testUser.getUsername())
                .action(activityRequest.getAction())
                .details(activityRequest.getDetails())
                .timestamp(LocalDateTime.now())
                .build();

        pageable = PageRequest.of(0, 10);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void logActivity_Success() throws Exception {
        when(activityService.logActivity(eq(testUser.getId()), any(ActivityRequest.class)))
                .thenReturn(activityResponse);

        mockMvc.perform(post("/api/activities")
                .with(request -> {
                    request.setUserPrincipal(() -> "testuser");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityResponse.getId()))
                .andExpect(jsonPath("$.userId").value(activityResponse.getUserId()))
                .andExpect(jsonPath("$.username").value(activityResponse.getUsername()))
                .andExpect(jsonPath("$.action").value(activityResponse.getAction()))
                .andExpect(jsonPath("$.details").value(activityResponse.getDetails()));
    }

    @Test
    @WithMockUser(username = "testuser")
    void logActivity_InvalidRequest() throws Exception {
        activityRequest.setAction(""); // Invalid empty action

        mockMvc.perform(post("/api/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getRecentActivities_Success() throws Exception {
        List<ActivityResponse> activities = Arrays.asList(activityResponse);
        Page<ActivityResponse> activityPage = new PageImpl<>(activities, pageable, activities.size());

        when(activityService.getRecentActivities(any(Pageable.class)))
                .thenReturn(activityPage);

        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(activityResponse.getId()))
                .andExpect(jsonPath("$.content[0].action").value(activityResponse.getAction()))
                .andExpect(jsonPath("$.content[0].details").value(activityResponse.getDetails()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getRecentActivities_NoContent() throws Exception {
        Page<ActivityResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(activityService.getRecentActivities(any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getActivitiesByUserId_Success() throws Exception {
        List<ActivityResponse> activities = Arrays.asList(activityResponse);
        Page<ActivityResponse> activityPage = new PageImpl<>(activities, pageable, activities.size());

        when(activityService.getActivitiesByUserId(eq(testUser.getId()), any(Pageable.class)))
                .thenReturn(activityPage);

        mockMvc.perform(get("/api/activities/user/{userId}", testUser.getId())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(activityResponse.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(activityResponse.getUserId()))
                .andExpect(jsonPath("$.content[0].action").value(activityResponse.getAction()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getActivitiesByUserId_UserNotFound() throws Exception {
        when(activityService.getActivitiesByUserId(eq(999L), any(Pageable.class)))
                .thenThrow(new UserNotFoundException("User with ID 999 not found"));

        mockMvc.perform(get("/api/activities/user/{userId}", 999L)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchActivities_Success() throws Exception {
        List<ActivityResponse> activities = Arrays.asList(activityResponse);
        Page<ActivityResponse> activityPage = new PageImpl<>(activities, pageable, activities.size());
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(activityService.searchActivities(
                eq(testUser.getId()),
                eq(startTime),
                eq(endTime),
                any(Pageable.class)))
                .thenReturn(activityPage);

        mockMvc.perform(get("/api/activities/search")
                .param("userId", testUser.getId().toString())
                .param("startTime", startTime.toString())
                .param("endTime", endTime.toString())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(activityResponse.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(activityResponse.getUserId()))
                .andExpect(jsonPath("$.content[0].action").value(activityResponse.getAction()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchActivities_UserNotFound() throws Exception {
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(activityService.searchActivities(
                eq(999L),
                eq(startTime),
                eq(endTime),
                any(Pageable.class)))
                .thenThrow(new UserNotFoundException("User with ID 999 not found"));

        mockMvc.perform(get("/api/activities/search")
                .param("userId", "999")
                .param("startTime", startTime.toString())
                .param("endTime", endTime.toString())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchActivities_WithoutUserId() throws Exception {
        List<ActivityResponse> activities = Arrays.asList(activityResponse);
        Page<ActivityResponse> activityPage = new PageImpl<>(activities, pageable, activities.size());
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(activityService.searchActivities(
                eq(null),
                eq(startTime),
                eq(endTime),
                any(Pageable.class)))
                .thenReturn(activityPage);

        mockMvc.perform(get("/api/activities/search")
                .param("startTime", startTime.toString())
                .param("endTime", endTime.toString())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(activityResponse.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(activityResponse.getUserId()))
                .andExpect(jsonPath("$.content[0].action").value(activityResponse.getAction()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }
} 