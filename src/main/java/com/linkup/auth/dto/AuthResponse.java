package com.linkup.auth.dto;

import com.linkup.user.dto.UserDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserDto user
) {
}
