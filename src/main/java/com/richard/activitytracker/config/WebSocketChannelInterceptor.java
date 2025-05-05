package com.richard.activitytracker.config;

import com.richard.activitytracker.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token != null) {
                try {
                    Authentication auth = jwtAuthenticationFilter.getAuthentication(token);
                    if (auth != null) {
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.info("WebSocket connection authenticated for user: {}", auth.getName());
                        return message;
                    }
                } catch (Exception e) {
                    log.error("WebSocket authentication failed: {}", e.getMessage());
                }
            }
            return null; // Reject the message
        }
        
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
} 