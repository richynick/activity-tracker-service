package com.richard.activitytracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    
    @Bean
    public JwtService jwtService() {
        return new JwtService();
    }
} 