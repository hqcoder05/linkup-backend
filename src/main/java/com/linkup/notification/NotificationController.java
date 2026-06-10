package com.linkup.notification;

import com.linkup.common.ApiResponse;
import com.linkup.notification.dto.NotificationDto;
import com.linkup.security.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notificationService, CurrentUser currentUser) {
        this.notificationService = notificationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    ApiResponse<List<NotificationDto>> mine(Authentication authentication) {
        return ApiResponse.ok(notificationService.forUser(currentUser.id(authentication)));
    }

    @GetMapping("/unread-count")
    ApiResponse<Map<String, Long>> unreadCount(Authentication authentication) {
        return ApiResponse.ok(Map.of("unreadCount", notificationService.unreadCount(currentUser.id(authentication))));
    }

    @PostMapping("/{id}/read")
    ApiResponse<Void> read(@PathVariable Long id, Authentication authentication) {
        notificationService.markRead(id, currentUser.id(authentication));
        return ApiResponse.message("Marked as read");
    }

    @PostMapping("/read-all")
    ApiResponse<Void> readAll(Authentication authentication) {
        notificationService.markAllRead(currentUser.id(authentication));
        return ApiResponse.message("All notifications marked as read");
    }
}
