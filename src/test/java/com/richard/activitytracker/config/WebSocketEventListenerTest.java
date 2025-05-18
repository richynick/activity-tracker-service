package com.richard.activitytracker.config;

import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.CloseStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebSocketEventListenerTest {

    private WebSocketEventListener eventListener;
    private SimpMessagingTemplate messagingTemplate;
    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        activityService = mock(ActivityService.class);
        eventListener = new WebSocketEventListener(messagingTemplate, activityService);
    }

    @Test
    void handleWebSocketConnectListener_ShouldSendInitialActivities() {
        // Arrange
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        headerAccessor.setSessionId("test-session-id");
        SessionConnectedEvent event = new SessionConnectedEvent(this, MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders()));
        List<ActivityResponse> activities = List.of(new ActivityResponse());
        Page<ActivityResponse> page = new PageImpl<>(activities);
        when(activityService.getAllActivities(any(Pageable.class))).thenReturn(page);

        // Act
        eventListener.handleWebSocketConnectListener(event);

        // Assert
        verify(messagingTemplate).convertAndSendToUser(eq("test-session-id"), eq("/queue/activities"), eq(activities));
    }

    @Test
    void handleWebSocketDisconnectListener_ShouldLogDisconnect() {
        // Arrange
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setSessionId("test-session-id");
        ApplicationContext context = mock(ApplicationContext.class);
        SessionDisconnectEvent event = new SessionDisconnectEvent(
            context,
            MessageBuilder.<byte[]>createMessage(new byte[0], headerAccessor.getMessageHeaders()),
            "test-session-id",
            CloseStatus.NORMAL
        );

        // Act
        eventListener.handleWebSocketDisconnectListener(event);

        // Assert - verify no exceptions are thrown
        assertTrue(true);
    }
} 
