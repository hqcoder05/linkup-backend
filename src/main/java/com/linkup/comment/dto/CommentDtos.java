package com.linkup.comment.dto;

import com.linkup.user.dto.UserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class CommentDtos {
    private CommentDtos() {
    }

    public record CommentRequest(@NotBlank @Size(max = 1000) String content) {
    }

    public record CommentDto(
            Long id,
            UserDto user,
            String content,
            long likesCount,
            boolean likedByCurrentUser,
            Instant createdAt
    ) {
    }
}
