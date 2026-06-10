package com.linkup.notification.dto;

import java.time.Instant;

public record NotificationDto(
        Long id,
        String type,
        String title,
        String content,
        String url,
        String targetId,
        Long lastInteractorId,
        int interactionCount,
        boolean read,
        Instant createdAt
) {
}
