package com.richard.activitytracker.service.impl;

import com.richard.activitytracker.dto.AuthRequest;
import com.richard.activitytracker.dto.AuthResponse;
import com.richard.activitytracker.dto.RegisterRequest;
import com.richard.activitytracker.exception.TokenGenerationException;
import com.richard.activitytracker.handler.ErrorResponse;
import com.richard.activitytracker.model.User;
import com.richard.activitytracker.repository.UserRepository;
import com.richard.activitytracker.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
                throw new IllegalArgumentException(format("User with username %s already exists", request.getUsername()));
            } else if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Registration failed - email already exists: {}", request.getEmail());
                throw new IllegalArgumentException(format("User with email %s already exists", request.getEmail()));
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
        } catch (IllegalArgumentException e) {
            log.error("Registration failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            throw new RuntimeException("Registration failed due to an unexpected error", e);
        }
    }

    public ResponseEntity<?> login(AuthRequest request) {
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
                        return new BadCredentialsException("Invalid username or password");
                    });
            
            log.info("User authenticated successfully: {}", user.getUsername());
            
            try {
                String jwtToken = jwtService.generateToken(user);
                return ResponseEntity.ok(new AuthResponse(jwtToken));
            } catch (Exception e) {
                log.error("Token generation failed for user: {}", user.getUsername(), e);
                throw new TokenGenerationException("Failed to generate authentication token", e);
            }
        } catch (BadCredentialsException e) {
            log.warn("Login failed - invalid credentials for user: {}", request.getUsername());
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ErrorResponse(
                            "Invalid username or password",
                            "Authentication failed",
                            UNAUTHORIZED.value(),
                            getCurrentRequestPath()
                    ));
        } catch (ExpiredJwtException e) {
            log.warn("Login failed - token expired: {}", e.getMessage());
            Map<String, String> details = new HashMap<>();
            details.put("expiredAt", e.getClaims().getExpiration().toString());
            details.put("currentTime", e.getMessage().split("Current time: ")[1].split(",")[0]);
            details.put("difference", e.getMessage().split("difference of ")[1].split(" milliseconds")[0] + " milliseconds");
            
            return ResponseEntity.status(UNAUTHORIZED)
                    .body(new ErrorResponse(
                            "Your session has expired. Please log in again.",
                            "Token expired",
                            UNAUTHORIZED.value(),
                            getCurrentRequestPath(),
                            details
                    ));
        } catch (Exception e) {
            log.error("Unexpected error during login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(
                            "Login failed due to an unexpected error",
                            "Internal server error",
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            getCurrentRequestPath()
                    ));
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
