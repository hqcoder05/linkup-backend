package com.linkup.follow.dto;

import com.linkup.user.dto.UserDto;
import java.time.Instant;

public record FollowDto(
        UserDto follower,
        UserDto following,
        String status,
        Instant createdAt,
        Instant approvedAt
) {
}
