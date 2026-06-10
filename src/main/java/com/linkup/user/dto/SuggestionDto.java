package com.linkup.user.dto;

import java.util.List;

public record SuggestionDto(
        UserDto user,
        int mutualCount,
        List<String> mutualFriendNames
) {
}
