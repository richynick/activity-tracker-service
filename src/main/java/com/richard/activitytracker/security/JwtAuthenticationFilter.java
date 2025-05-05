package com.richard.activitytracker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.activitytracker.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader("Authorization");
            final String jwt;
            final String userEmail;

            // Skip JWT validation for login and register endpoints
            if (request.getRequestURI().startsWith("/api/auth/")) {
                filterChain.doFilter(request, response);
                return;
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("No JWT token found in request");
                filterChain.doFilter(request, response);
                return;
            }

            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Successfully authenticated user: {}", userEmail);
                } else {
                    log.warn("Invalid JWT token for user: {}", userEmail);
                }
            }
            filterChain.doFilter(request, response);
        } catch (TokenExpiredException e) {
            log.warn("Token expired: {}", e.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .error("Unauthorized")
                    .message("Your session has expired. Please log in again.")
                    .path(request.getRequestURI())
                    .build();
            
            objectMapper.writeValue(response.getWriter(), errorResponse);
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .error("Unauthorized")
                    .message("Invalid authentication token")
                    .path(request.getRequestURI())
                    .build();
            
            objectMapper.writeValue(response.getWriter(), errorResponse);
        }
    }

    public Authentication getAuthentication(String token) {
        try {
            String username = jwtService.extractUsername(token);
            if (username != null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, userDetails)) {
                    return new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error getting authentication from token: {}", e.getMessage());
        }
        return null;
    }
} 