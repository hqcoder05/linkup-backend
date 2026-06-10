package com.linkup.profile.dto;

import com.linkup.user.dto.UserDto;
import java.time.Instant;

public record ProfileDto(
        Long id,
        UserDto user,
        String nickname,
        String bio,
        String headline,
        String location,
        String websiteUrl,
        long postsCount,
        long followersCount,
        long followingCount,
        Instant createdAt,
        Instant updatedAt
) {
}
