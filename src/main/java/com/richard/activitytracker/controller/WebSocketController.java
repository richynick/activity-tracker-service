package com.richard.activitytracker.controller;

import com.richard.activitytracker.dto.ActivityResponse;
import com.richard.activitytracker.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {
    private final ActivityService activityService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/activities")
    @SendTo("/topic/activities")
    public List<ActivityResponse> getActivities() {
        log.info("Received request for activities");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("timestamp").descending());
        return activityService.getAllActivities(pageable).getContent();
    }

    @SubscribeMapping("/topic/activities")
    public List<ActivityResponse> handleActivitySubscription() {
        log.info("New subscription to activities topic");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("timestamp").descending());
        return activityService.getAllActivities(pageable).getContent();
    }

    @MessageMapping("/user/activities")
    public void handleUserActivity(ActivityResponse activity) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String username = auth.getName();
            log.info("Received user activity from {}: {}", username, activity);
            messagingTemplate.convertAndSendToUser(username, "/queue/activities", activity);
        }
    }

    @SubscribeMapping("/user/queue/activities")
    public List<ActivityResponse> handleUserActivitySubscription() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String username = auth.getName();
            log.info("New subscription to user activities queue for user: {}", username);
            Pageable pageable = PageRequest.of(0, 10, Sort.by("timestamp").descending());
            return activityService.getAllActivities(pageable).getContent();
        }
        return List.of();
    }
} 