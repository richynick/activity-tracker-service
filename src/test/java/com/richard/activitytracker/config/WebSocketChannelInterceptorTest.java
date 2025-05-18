package com.richard.activitytracker.config;

import com.richard.activitytracker.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketChannelInterceptorTest {

    private WebSocketChannelInterceptor interceptor;
    private JwtAuthenticationFilter jwtAuthFilter;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = mock(JwtAuthenticationFilter.class);
        interceptor = new WebSocketChannelInterceptor(jwtAuthFilter);
        channel = mock(MessageChannel.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void preSend_WithValidToken_ShouldAuthenticate() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer valid-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        Authentication auth = mock(Authentication.class);
        when(jwtAuthFilter.getAuthentication("valid-token")).thenReturn(auth);

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertNotNull(result);
        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
        verify(jwtAuthFilter).getAuthentication("valid-token");
    }

    @Test
    void preSend_WithInvalidToken_ShouldReject() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer invalid-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        when(jwtAuthFilter.getAuthentication("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertNull(result);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtAuthFilter).getAuthentication("invalid-token");
    }

    @Test
    void preSend_WithNoToken_ShouldReject() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertNull(result);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtAuthFilter, never()).getAuthentication(anyString());
    }

    @Test
    void preSend_WithMalformedToken_ShouldReject() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "InvalidFormat");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertNull(result);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtAuthFilter, never()).getAuthentication(anyString());
    }

    @Test
    void preSend_WithNonConnectCommand_ShouldPassThrough() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertNotNull(result);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtAuthFilter, never()).getAuthentication(anyString());
    }

//    @Test
//    void preSend_WithNullHeaders_ShouldPassThrough() {
//        // Arrange
//        Message<?> message = MessageBuilder.createMessage(new byte[0], null);
//
//        // Act
//        Message<?> result = interceptor.preSend(message, channel);
//
//        // Assert
//        assertNotNull(result);
//        assertNull(SecurityContextHolder.getContext().getAuthentication());
//        verify(jwtAuthFilter, never()).getAuthentication(anyString());
//    }
} 
