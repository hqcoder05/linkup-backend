package com.linkup.post.dto;

import com.linkup.user.dto.UserDto;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class PostDtos {
    private PostDtos() {
    }

    public record CreatePostRequest(
            @Size(max = 2000) String caption,
            @Size(max = 700) String imageUrl,
            @Size(max = 700) String videoUrl
    ) {
    }

    public record UpdatePostRequest(@Size(max = 2000) String caption) {
    }

    public record PostDto(
            Long id,
            UserDto user,
            String caption,
            String imageUrl,
            String videoUrl,
            long likesCount,
            long commentsCount,
            boolean likedByCurrentUser,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
