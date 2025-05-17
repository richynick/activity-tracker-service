package com.richard.activitytracker.config;

import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ActivityService activityService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Received a new web socket connection");
        // Send recent activities to the newly connected client
        Pageable pageable = PageRequest.of(0, 10, Sort.by("timestamp").descending());
        List<ActivityResponse> activities = activityService.getAllActivities(pageable).getContent();
        messagingTemplate.convertAndSend("/topic/activities", activities);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info("User disconnected");
    }
} 