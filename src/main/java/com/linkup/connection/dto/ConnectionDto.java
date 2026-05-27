package com.linkup.connection.dto;

import com.linkup.user.dto.UserDto;
import java.time.Instant;

public record ConnectionDto(
        UserDto requester,
        UserDto addressee,
        String status,
        Instant createdAt,
        Instant respondedAt
) {
}
