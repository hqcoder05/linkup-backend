package com.linkup.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linkup.notification.dto.NotificationDto;
import com.linkup.user.User;
import com.linkup.user.UserService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserService userService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createsFirstAggregatedNotificationForPostLike() {
        User owner = user(1L, "Owner");
        User userA = user(2L, "User A");

        when(userService.get(1L)).thenReturn(owner);
        when(userService.get(2L)).thenReturn(userA);
        when(notificationRepository.findFirstByUserIdAndTypeAndTargetIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(1L), eq("post_like"), eq("99"), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(10L);
            return notification;
        });

        NotificationDto dto = notificationService.create(1L, "post_like", "New like", "ignored", "/posts/99", "99", 2L);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.content()).isEqualTo("User A liked your post.");
        assertThat(dto.interactionCount()).isEqualTo(1);
        assertThat(dto.targetId()).isEqualTo("99");
        assertThat(dto.lastInteractorId()).isEqualTo(2L);
        verify(messagingTemplate).convertAndSend("/topic/notifications/1", dto);
    }

    @Test
    void updatesExistingAggregatedNotificationForSecondPostLike() {
        User owner = user(1L, "Owner");
        User userA = user(2L, "User A");
        User userB = user(3L, "User B");
        Notification existing = notification(10L, owner, userA, 1);

        when(userService.get(1L)).thenReturn(owner);
        when(userService.get(3L)).thenReturn(userB);
        when(notificationRepository.findFirstByUserIdAndTypeAndTargetIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(1L), eq("post_like"), eq("99"), any(Instant.class)))
                .thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDto dto = notificationService.create(1L, "post_like", "New like", "ignored", "/posts/99", "99", 3L);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.content()).isEqualTo("User B and User A liked your post.");
        assertThat(dto.interactionCount()).isEqualTo(2);
        assertThat(dto.lastInteractorId()).isEqualTo(3L);
        verify(messagingTemplate).convertAndSend("/topic/notifications/1", dto);
    }

    @Test
    void updatesExistingAggregatedNotificationForThirdPostLike() {
        User owner = user(1L, "Owner");
        User userB = user(3L, "User B");
        User userC = user(4L, "User C");
        Notification existing = notification(10L, owner, userB, 2);

        when(userService.get(1L)).thenReturn(owner);
        when(userService.get(4L)).thenReturn(userC);
        when(notificationRepository.findFirstByUserIdAndTypeAndTargetIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(1L), eq("post_like"), eq("99"), any(Instant.class)))
                .thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDto dto = notificationService.create(1L, "post_like", "New like", "ignored", "/posts/99", "99", 4L);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.content()).isEqualTo("User C, User B and 1 other liked your post.");
        assertThat(dto.interactionCount()).isEqualTo(3);
        assertThat(dto.lastInteractorId()).isEqualTo(4L);
    }

    @Test
    void decrementsAggregatedNotificationOnUnlike() {
        User owner = user(1L, "Owner");
        User userC = user(4L, "User C");
        Notification existing = notification(10L, owner, userC, 3);

        when(notificationRepository.findFirstByUserIdAndTypeAndTargetIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(1L), eq("post_like"), eq("99"), any(Instant.class)))
                .thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.decrementInteraction(1L, "post_like", "99", 2L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getInteractionCount()).isEqualTo(2);
        assertThat(captor.getValue().getContent()).isEqualTo("2 people liked your post.");
    }

    private User user(Long id, String fullName) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(fullName.toLowerCase().replace(" ", ".") + "@example.com");
        user.setPasswordHash("hash");
        return user;
    }

    private Notification notification(Long id, User owner, User lastInteractor, int count) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUser(owner);
        notification.setType("post_like");
        notification.setTitle("New like");
        notification.setContent("old");
        notification.setUrl("/posts/99");
        notification.setTargetId("99");
        notification.setLastInteractor(lastInteractor);
        notification.setInteractionCount(count);
        notification.setCreatedAt(Instant.now());
        return notification;
    }
}
