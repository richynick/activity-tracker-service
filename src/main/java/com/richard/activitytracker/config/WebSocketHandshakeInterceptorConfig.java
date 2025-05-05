package com.richard.activitytracker.config;

import com.richard.activitytracker.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptorConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public WebSocketHandshakeInterceptor webSocketHandshakeInterceptor() {
        return new WebSocketHandshakeInterceptor(jwtAuthenticationFilter);
    }
}
