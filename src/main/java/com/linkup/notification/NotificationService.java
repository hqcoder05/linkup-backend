package com.linkup.notification;

import com.linkup.notification.dto.NotificationDto;
import com.linkup.user.User;
import com.linkup.user.UserService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private static final Set<String> AGGREGATED_TYPES = Set.of("post_like", "post_comment");
    private static final Duration AGGREGATION_WINDOW = Duration.ofDays(7);

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
        return create(userId, type, title, content, url, null, null);
    }

    @Transactional
    public NotificationDto create(Long userId, String type, String title, String content, String url, String targetId, Long interactorId) {
        if (canAggregate(type, targetId, interactorId)) {
            return createAggregated(userId, type, title, url, targetId, interactorId);
        }

        User user = userService.get(userId);
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setUrl(url);
        notification.setTargetId(targetId);
        NotificationDto dto = toDto(notificationRepository.save(notification));
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, dto);
        return dto;
    }

    @Transactional
    public void decrementInteraction(Long userId, String type, String targetId, Long interactorId) {
        if (!canAggregate(type, targetId, interactorId)) {
            return;
        }
        Instant windowStart = Instant.now().minus(AGGREGATION_WINDOW);
        notificationRepository
                .findFirstByUserIdAndTypeAndTargetIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(userId, type, targetId, windowStart)
                .ifPresent(notification -> {
                    int nextCount = notification.getInteractionCount() - 1;
                    if (nextCount <= 0) {
                        notificationRepository.delete(notification);
                        return;
                    }
                    notification.setInteractionCount(nextCount);
                    notification.setContent(buildReducedContent(type, notification.getLastInteractor(), nextCount));
                    notification.setCreatedAt(Instant.now());
                    NotificationDto dto = toDto(notificationRepository.save(notification));
                    messagingTemplate.convertAndSend("/topic/notifications/" + userId, dto);
                });
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

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getUrl(),
                notification.getTargetId(),
                notification.getLastInteractor() == null ? null : notification.getLastInteractor().getId(),
                notification.getInteractionCount(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    private NotificationDto createAggregated(Long userId, String type, String title, String url, String targetId, Long interactorId) {
        User owner = userService.get(userId);
        User interactor = userService.get(interactorId);
        Instant now = Instant.now();
        Instant windowStart = now.minus(AGGREGATION_WINDOW);
        Notification notification = notificationRepository
                .findFirstByUserIdAndTypeAndTargetIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(userId, type, targetId, windowStart)
                .map(existing -> updateAggregated(existing, interactor, now))
                .orElseGet(() -> newAggregated(owner, type, title, url, targetId, interactor, now));

        NotificationDto dto = toDto(notificationRepository.save(notification));
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, dto);
        return dto;
    }

    private Notification updateAggregated(Notification notification, User interactor, Instant now) {
        User previousInteractor = notification.getLastInteractor();
        int nextCount = notification.getInteractionCount() + 1;
        notification.setInteractionCount(nextCount);
        notification.setLastInteractor(interactor);
        notification.setContent(buildAggregatedContent(notification.getType(), interactor, previousInteractor, nextCount));
        notification.setCreatedAt(now);
        return notification;
    }

    private Notification newAggregated(User owner, String type, String title, String url, String targetId, User interactor, Instant now) {
        Notification notification = new Notification();
        notification.setUser(owner);
        notification.setType(type);
        notification.setTitle(title);
        notification.setUrl(url);
        notification.setTargetId(targetId);
        notification.setLastInteractor(interactor);
        notification.setInteractionCount(1);
        notification.setContent(singleInteractionContent(type, interactor));
        notification.setCreatedAt(now);
        return notification;
    }

    private boolean canAggregate(String type, String targetId, Long interactorId) {
        return AGGREGATED_TYPES.contains(type) && targetId != null && interactorId != null;
    }

    private String buildAggregatedContent(String type, User currentInteractor, User previousInteractor, int count) {
        if (count <= 1 || previousInteractor == null) {
            return singleInteractionContent(type, currentInteractor);
        }
        String action = actionText(type);
        if (count == 2) {
            return currentInteractor.getFullName() + " and " + previousInteractor.getFullName() + " " + action + " your post.";
        }
        return currentInteractor.getFullName() + ", " + previousInteractor.getFullName() + " and " + othersText(count - 2) + " " + action + " your post.";
    }

    private String buildReducedContent(String type, User lastInteractor, int count) {
        String action = actionText(type);
        if (count <= 1 && lastInteractor != null) {
            return singleInteractionContent(type, lastInteractor);
        }
        return count + " people " + action + " your post.";
    }

    private String singleInteractionContent(String type, User interactor) {
        return interactor.getFullName() + " " + actionText(type) + " your post.";
    }

    private String actionText(String type) {
        return "post_comment".equals(type) ? "commented on" : "liked";
    }

    private String othersText(int count) {
        return count == 1 ? "1 other" : count + " others";
    }
}
