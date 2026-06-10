package com.linkup.post.dto;

import com.linkup.user.dto.UserDto;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class PostDtos {
    private PostDtos() {
    }

    public record CreatePostRequest(
            @Size(max = 2000) String caption,
            List<PostMediaRequest> media,
            List<TagUserRequest> tags
    ) {
    }

    public record UpdatePostRequest(@Size(max = 2000) String caption) {
    }

    public record PostDto(
            Long id,
            UserDto user,
            String caption,
            List<PostMediaDto> media,
            List<TaggedUserDto> taggedUsers,
            List<String> hashtags,
            long likesCount,
            long commentsCount,
            boolean likedByCurrentUser,
            boolean savedByCurrentUser,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PostMediaRequest(
            @Size(max = 700) String url,
            @Size(max = 700) String thumbnailUrl,
            @Size(max = 50) String type,
            Integer width,
            Integer height
    ) {
    }

    public record TagUserRequest(
            Long userId,
            int mediaPosition,
            Double x,
            Double y
    ) {
    }

    public record PostMediaDto(
            Long id,
            String url,
            String thumbnailUrl,
            String type,
            int position,
            Integer width,
            Integer height
    ) {
    }

    public record TaggedUserDto(
            UserDto user,
            int mediaPosition,
            Double x,
            Double y
    ) {
    }
}
