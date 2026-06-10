package com.linkup.user;

import com.linkup.user.dto.UserDto;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getCoverUrl(),
                user.isPrivateAccount(),
                false,
                false,
                user.getRole().name(),
                user.getCreatedAt());
    }

    public static UserDto toDto(User user, boolean following) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getCoverUrl(),
                user.isPrivateAccount(),
                following,
                following,
                user.getRole().name(),
                user.getCreatedAt());
    }
}

