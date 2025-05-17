package com.richard.activitytracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.activitytracker.config.TestConfig;
import com.richard.activitytracker.config.TestSecurityConfig;
import com.richard.activitytracker.dto.AuthRequest;
import com.richard.activitytracker.dto.AuthResponse;
import com.richard.activitytracker.dto.RegisterRequest;
import com.richard.activitytracker.exception.AuthenticationException;
import com.richard.activitytracker.model.Role;
import com.richard.activitytracker.security.JwtService;
import com.richard.activitytracker.service.impl.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({TestSecurityConfig.class, TestConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest registerRequest;
    private AuthRequest authRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(Role.USER);

        authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");

        authResponse = new AuthResponse("jwtToken");
    }

    @Test
    void register_Success() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwtToken"));
    }

    @Test
    void register_InvalidRequest() throws Exception {
        registerRequest.setUsername(""); // Invalid username

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_Success() throws Exception {
        when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwtToken"));
    }

    @Test
    void login_InvalidCredentials() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new AuthenticationException(
                    "Invalid username or password",
                    "Authentication failed",
                    HttpStatus.UNAUTHORIZED.value(),
                    "/api/auth/login"
                ));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.error").value("Authentication failed"));
    }

    @Test
    void login_TokenExpired() throws Exception {
        Map<String, String> details = new HashMap<>();
        details.put("expiredAt", "2024-03-20T10:00:00.000");
        details.put("currentTime", "2024-03-20T11:00:00.000");
        details.put("difference", "3600000 milliseconds");

        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new AuthenticationException(
                    "Your session has expired. Please log in again.",
                    "Token expired",
                    HttpStatus.UNAUTHORIZED.value(),
                    "/api/auth/login",
                    details
                ));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your session has expired. Please log in again."))
                .andExpect(jsonPath("$.error").value("Token expired"))
                .andExpect(jsonPath("$.errors.expiredAt").exists())
                .andExpect(jsonPath("$.errors.currentTime").exists())
                .andExpect(jsonPath("$.errors.difference").exists());
    }

    @Test
    void login_InvalidRequest() throws Exception {
        authRequest.setUsername(""); // Invalid username

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest());
    }
} 
