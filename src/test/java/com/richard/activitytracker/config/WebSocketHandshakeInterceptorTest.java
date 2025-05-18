package com.richard.activitytracker.config;

import com.richard.activitytracker.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketHandshakeInterceptorTest {

    private WebSocketHandshakeInterceptor interceptor;
    private JwtAuthenticationFilter jwtAuthFilter;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private WebSocketHandler wsHandler;
    private Map<String, Object> attributes;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = mock(JwtAuthenticationFilter.class);
        interceptor = new WebSocketHandshakeInterceptor(jwtAuthFilter);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        wsHandler = mock(WebSocketHandler.class);
        attributes = new HashMap<>();
        headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
    }

    @Test
    void beforeHandshake_WithValidQueryToken_ShouldAllowHandshake() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws?token=valid-token"));
        Authentication auth = mock(Authentication.class);
        when(jwtAuthFilter.getAuthentication("valid-token")).thenReturn(auth);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertTrue(result);
        assertNotNull(attributes.get("user"));
        verify(jwtAuthFilter).getAuthentication("valid-token");
    }

    @Test
    void beforeHandshake_WithValidHeaderToken_ShouldAllowHandshake() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws"));
        headers.add("Authorization", "Bearer valid-token");
        Authentication auth = mock(Authentication.class);
        when(jwtAuthFilter.getAuthentication("valid-token")).thenReturn(auth);

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertTrue(result);
        assertNotNull(attributes.get("user"));
        verify(jwtAuthFilter).getAuthentication("valid-token");
    }

    @Test
    void beforeHandshake_WithInvalidToken_ShouldRejectHandshake() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws?token=invalid-token"));
        when(jwtAuthFilter.getAuthentication("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertFalse(result);
        assertNull(attributes.get("user"));
        verify(jwtAuthFilter).getAuthentication("invalid-token");
    }

    @Test
    void beforeHandshake_WithNoToken_ShouldRejectHandshake() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws"));

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertFalse(result);
        assertNull(attributes.get("user"));
        verify(jwtAuthFilter, never()).getAuthentication(anyString());
    }

    @Test
    void beforeHandshake_WithMalformedHeaderToken_ShouldRejectHandshake() {
        // Arrange
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws"));
        headers.add("Authorization", "InvalidFormat");

        // Act
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Assert
        assertFalse(result);
        assertNull(attributes.get("user"));
        verify(jwtAuthFilter, never()).getAuthentication(anyString());
    }

    @Test
    void afterHandshake_ShouldLogSuccess() {
        // Act
        interceptor.afterHandshake(request, response, wsHandler, null);

        // Assert - verify no exceptions are thrown
        assertTrue(true);
    }

    @Test
    void afterHandshake_WithException_ShouldLogError() {
        // Arrange
        Exception exception = new RuntimeException("Test exception");

        // Act
        interceptor.afterHandshake(request, response, wsHandler, exception);

        // Assert - verify no exceptions are thrown
        assertTrue(true);
    }
} 