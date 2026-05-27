package com.linkup.media.dto;

import java.time.Instant;

public record MediaDto(
        Long id,
        String url,
        String type,
        String originalFilename,
        Long fileSize,
        Instant createdAt
) {
}
