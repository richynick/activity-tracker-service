package com.richard.activitytracker.service.impl;

import com.richard.activitytracker.dto.AuthRequest;
import com.richard.activitytracker.dto.AuthResponse;
import com.richard.activitytracker.dto.RegisterRequest;
import com.richard.activitytracker.exception.AuthenticationException;
import com.richard.activitytracker.exception.TokenGenerationException;
import com.richard.activitytracker.model.User;
import com.richard.activitytracker.repository.UserRepository;
import com.richard.activitytracker.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        try {
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("Registration failed - username already exists: {}", request.getUsername());
                throw new AuthenticationException(
                    format("User with username %s already exists", request.getUsername()),
                    "Registration failed",
                    HttpStatus.CONFLICT.value(),
                    "/api/auth/register"
                );
            } else if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Registration failed - email already exists: {}", request.getEmail());
                throw new AuthenticationException(
                    format("User with email %s already exists", request.getEmail()),
                    "Registration failed",
                    HttpStatus.CONFLICT.value(),
                    "/api/auth/register"
                );
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole());

            user = userRepository.save(user);
            log.info("User registered successfully: {}", user.getUsername());
            
            try {
                String jwtToken = jwtService.generateToken(user);
                return new AuthResponse(jwtToken);
            } catch (Exception e) {
                log.error("Token generation failed for user: {}", user.getUsername(), e);
                throw new TokenGenerationException("Failed to generate authentication token", e);
            }
        } catch (AuthenticationException e) {
            log.error("Registration failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            throw new RuntimeException("Registration failed due to an unexpected error", e);
        }
    }

    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        log.error("Login failed - user not found: {}", request.getUsername());
                        throw new AuthenticationException(
                            "Invalid username or password",
                            "Authentication failed",
                            UNAUTHORIZED.value(),
                            getCurrentRequestPath()
                        );
                    });
            
            log.info("User authenticated successfully: {}", user.getUsername());
            
            try {
                String jwtToken = jwtService.generateToken(user);
                return new AuthResponse(jwtToken);
            } catch (Exception e) {
                log.error("Token generation failed for user: {}", user.getUsername(), e);
                throw new TokenGenerationException("Failed to generate authentication token", e);
            }
        } catch (BadCredentialsException e) {
            log.warn("Login failed - invalid credentials for user: {}", request.getUsername());
            throw new AuthenticationException(
                "Invalid username or password",
                "Authentication failed",
                UNAUTHORIZED.value(),
                getCurrentRequestPath()
            );
        } catch (TokenGenerationException e) {
            log.error("Token generation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login: {}", e.getMessage(), e);
            throw new AuthenticationException(
                "Invalid username or password",
                "Authentication failed",
                UNAUTHORIZED.value(),
                getCurrentRequestPath()
            );
        }
    }

    private String getCurrentRequestPath() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            return request.getRequestURI();
        } catch (Exception e) {
            log.warn("Could not get current request path: {}", e.getMessage());
            return "/api/auth/login";
        }
    }
}
