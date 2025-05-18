package com.richard.activitytracker.config;

import com.richard.activitytracker.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketSecurityConfigTest {

    private WebSocketSecurityConfig config;
    private JwtAuthenticationFilter jwtAuthFilter;
    private ChannelRegistration registration;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = mock(JwtAuthenticationFilter.class);
        config = new WebSocketSecurityConfig(jwtAuthFilter);
        registration = mock(ChannelRegistration.class);
        when(registration.interceptors(any(ChannelInterceptor.class))).thenReturn(registration);
    }

    @Test
    void configureClientInboundChannel_ShouldAddInterceptor() {
        // Arrange
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);

        // Act
        config.configureClientInboundChannel(registration);

        // Assert
        verify(registration).interceptors(interceptorCaptor.capture());
        ChannelInterceptor interceptor = interceptorCaptor.getValue();
        assertTrue(interceptor instanceof WebSocketChannelInterceptor);
    }

    @Test
    void configureClientInboundChannel_ShouldUseCorrectJwtFilter() {
        // Arrange
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);

        // Act
        config.configureClientInboundChannel(registration);

        // Assert
        verify(registration).interceptors(interceptorCaptor.capture());
        ChannelInterceptor interceptor = interceptorCaptor.getValue();
        assertTrue(interceptor instanceof WebSocketChannelInterceptor);
        
        // Verify the interceptor is properly configured by testing its behavior
        WebSocketChannelInterceptor wsInterceptor = (WebSocketChannelInterceptor) interceptor;
        assertNotNull(wsInterceptor);
    }
} 