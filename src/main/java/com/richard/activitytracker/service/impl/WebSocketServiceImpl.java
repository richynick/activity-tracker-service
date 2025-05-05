package com.richard.activitytracker.service.impl;

import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.service.ActivityService;
import com.richard.activitytracker.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {
    @Lazy
    private final SimpMessagingTemplate messagingTemplate;


    @Override
    public void broadcastActivity(ActivityResponse activityResponse) {
        log.info("Broadcasting activity: {}", activityResponse);
        messagingTemplate.convertAndSend("/topic/activities", activityResponse);
        messagingTemplate.convertAndSend("/queue/activities", activityResponse);
    }
//    public void broadcastActivities(List<ActivityResponse> activityResponses) {
//        log.info("Broadcasting activities: {}", activityResponses);
//        messagingTemplate.convertAndSend("/topic/activities", activityResponses);
//        messagingTemplate.convertAndSend("/queue/activities", activityResponses);
//    }
} 