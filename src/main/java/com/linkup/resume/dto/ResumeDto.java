package com.linkup.resume.dto;

import java.time.Instant;

public record ResumeDto(
        Long id,
        String url,
        String originalFilename,
        String contentType,
        Long fileSize,
        Instant createdAt
) {
}
