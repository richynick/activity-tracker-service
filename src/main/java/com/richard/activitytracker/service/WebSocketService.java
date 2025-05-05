package com.richard.activitytracker.service;

import com.richard.activitytracker.dto.ActivityResponse;

public interface WebSocketService {
    void broadcastActivity(ActivityResponse activityResponse);
} 