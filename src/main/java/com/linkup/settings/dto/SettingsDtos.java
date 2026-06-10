package com.linkup.settings.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class SettingsDtos {
    private SettingsDtos() {
    }

    public record AccountSettingsDto(
            String phoneNumber,
            LocalDate dateOfBirth,
            boolean emailNotificationsEnabled,
            boolean pushNotificationsEnabled,
            boolean autoplayVideoEnabled,
            boolean contentVisibleToPublic,
            boolean searchIndexingEnabled,
            boolean twoFactorEnabled,
            boolean active,
            Instant deactivatedAt
    ) {
    }

    public record UpdateAccountSettingsRequest(
            @Size(max = 40) String phoneNumber,
            LocalDate dateOfBirth,
            Boolean emailNotificationsEnabled,
            Boolean pushNotificationsEnabled,
            Boolean autoplayVideoEnabled,
            Boolean contentVisibleToPublic,
            Boolean searchIndexingEnabled,
            Boolean twoFactorEnabled
    ) {
    }

    public record ChangePasswordRequest(
            @Size(min = 1, max = 120) String currentPassword,
            @Size(min = 8, max = 120) String newPassword
    ) {
    }

    public record SessionDto(
            Long id,
            Instant createdAt,
            Instant expiresAt,
            boolean current
    ) {
    }
}
