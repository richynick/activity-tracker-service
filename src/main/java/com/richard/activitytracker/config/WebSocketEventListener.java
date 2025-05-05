package com.richard.activitytracker.config;

import com.richard.activitytracker.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    @Lazy
    private final SimpMessagingTemplate messagingTemplate;
    @Lazy
    private final ActivityService activityService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("New WebSocket connection established: {}", sessionId);
        sendInitialActivities(sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket connection closed: {}", sessionId);
    }

    private void sendInitialActivities(String sessionId) {
        log.info("Sending initial activities to session: {}", sessionId);
        var activities = activityService.getAllActivities();
        log.info("Found {} activities to send", activities.size());
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/activities", activities);
    }
} 