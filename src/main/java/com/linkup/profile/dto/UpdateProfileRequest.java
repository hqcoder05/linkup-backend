package com.linkup.profile.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 120) String fullName,
        @Size(max = 120) String nickname,
        @Size(max = 1000) String bio,
        @Size(max = 120) String headline,
        @Size(max = 200) String location,
        @Size(max = 500) String websiteUrl,
        @Size(max = 500) String avatarUrl,
        @Size(max = 500) String coverUrl,
        Boolean privateAccount
) {
}
