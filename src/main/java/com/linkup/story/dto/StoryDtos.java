package com.linkup.story.dto;

import com.linkup.post.dto.PostDtos.PostMediaDto;
import com.linkup.user.dto.UserDto;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class StoryDtos {
    private StoryDtos() {
    }

    public record CreateStoryRequest(
            @Size(max = 700) String caption,
            List<StoryMediaRequest> media
    ) {
    }

    public record StoryMediaRequest(
            @Size(max = 700) String url,
            @Size(max = 700) String thumbnailUrl,
            @Size(max = 50) String type,
            Integer width,
            Integer height
    ) {
    }

    public record StoryDto(
            Long id,
            UserDto user,
            String caption,
            List<PostMediaDto> media,
            Instant createdAt,
            Instant expiresAt
    ) {
    }

    public record UserStoriesDto(
            UserDto user,
            List<StoryDto> stories,
            boolean hasUnseen,
            Instant latestStoryTime
    ) {
    }

    public record StoryViewerDto(
            UserDto user,
            Instant viewedAt
    ) {
    }
}
