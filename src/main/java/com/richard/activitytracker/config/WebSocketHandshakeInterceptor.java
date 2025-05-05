package com.richard.activitytracker.config;

import com.richard.activitytracker.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null) {
            log.warn("No token found in WebSocket handshake request");
            return false;
        }

        try {
            Authentication auth = jwtAuthenticationFilter.getAuthentication(token);
            if (auth != null) {
                log.info("WebSocket handshake validated for user: {}", auth.getName());
                attributes.put("user", auth);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("WebSocket handshake validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
            WebSocketHandler wsHandler, Exception exception) {
        if (exception == null) {
            log.info("WebSocket handshake completed successfully");
        }
    }

    private String extractToken(ServerHttpRequest request) {
        // First try to get token from query parameter
        String token = request.getURI().getQuery();
        if (token != null && token.startsWith("token=")) {
            return token.substring(6);
        }

        // Then try to get token from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
} 