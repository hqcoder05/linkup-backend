package com.linkup.notification;

import com.linkup.notification.dto.NotificationDto;
import com.linkup.user.User;
import com.linkup.user.UserService;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public NotificationDto create(Long userId, String type, String title, String content, String url) {
        User user = userService.get(userId);
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setUrl(url);
        NotificationDto dto = toDto(notificationRepository.save(notification));
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, dto);
        return dto;
    }

    public List<NotificationDto> forUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        notificationRepository.findById(id)
                .filter(n -> n.getUser().getId().equals(userId))
                .ifPresent(n -> n.setRead(true));
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getUrl(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
