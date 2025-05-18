package com.richard.activitytracker.config;

import com.richard.activitytracker.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.*;

class WebSocketConfigTest {

    private final JwtAuthenticationFilter jwtAuthFilter = mock(JwtAuthenticationFilter.class);

    @Test
    void configureMessageBroker_ShouldConfigureCorrectly() {
        // Arrange
        WebSocketConfig config = new WebSocketConfig(jwtAuthFilter);
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        SimpleBrokerRegistration brokerRegistration = mock(SimpleBrokerRegistration.class);
        when(registry.enableSimpleBroker("/topic")).thenReturn(brokerRegistration);
        when(registry.setApplicationDestinationPrefixes("/app")).thenReturn(registry);

        // Act
        config.configureMessageBroker(registry);

        // Assert
        verify(registry).enableSimpleBroker("/topic");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void registerStompEndpoints_ShouldRegisterCorrectly() {
        // Arrange
        WebSocketConfig config = new WebSocketConfig(jwtAuthFilter);
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration endpointRegistration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOrigins("*")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(any(WebSocketHandshakeInterceptor.class))).thenReturn(endpointRegistration);

        // Act
        config.registerStompEndpoints(registry);

        // Assert
        verify(registry).addEndpoint("/ws");
        verify(endpointRegistration).setAllowedOrigins("*");
        verify(endpointRegistration).addInterceptors(any(WebSocketHandshakeInterceptor.class));
    }
} 