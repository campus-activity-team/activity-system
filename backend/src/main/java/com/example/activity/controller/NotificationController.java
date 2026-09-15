package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.service.NotificationService;
import com.example.activity.vo.NotificationView;
import com.example.activity.vo.UnreadNotificationCountView;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationView>> list(Authentication authentication) {
        return ApiResponse.success(notificationService.list(authentication));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadNotificationCountView> unreadCount(Authentication authentication) {
        return ApiResponse.success(new UnreadNotificationCountView(notificationService.unreadCount(authentication)));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<NotificationView> markRead(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(notificationService.markRead(id, authentication));
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(authentication);
        return ApiResponse.success();
    }
}
