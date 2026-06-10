package com.linkup.chat.dto;

import com.linkup.user.dto.UserDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class ChatDtos {
    private ChatDtos() {
    }

    public record CreateConversationRequest(
            @NotEmpty List<Long> memberIds,
            @Size(max = 200) String name
    ) {
    }

    public record SendMessageRequest(
            @Size(max = 5000) String content,
            @Size(max = 700) String attachmentUrl,
            Long sharedPostId,
            Long sharedStoryId,
            Long disappearAfterSeconds
    ) {
    }

    public record ConversationDto(
            Long id,
            String name,
            boolean group,
            List<UserDto> members,
            MessageDto lastMessage,
            int unreadCount,
            Instant createdAt
    ) {
    }

    public record MessageDto(
            Long id,
            Long conversationId,
            UserDto sender,
            String content,
            String attachmentUrl,
            Long sharedPostId,
            Long sharedStoryId,
            boolean disappearing,
            Instant expiresAt,
            Instant createdAt,
            boolean deleted,
            boolean read,
            Instant readAt,
            boolean mine
    ) {
    }
}
