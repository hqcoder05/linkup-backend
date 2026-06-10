package com.linkup.user.dto;

import java.time.Instant;

public record UserDto(
        Long id,
        String email,
        String fullName,
        String avatarUrl,
        String coverUrl,
        boolean privateAccount,
        boolean following,
        boolean followedByCurrentUser,
        String role,
        Instant createdAt
) {
}

