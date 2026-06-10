package com.linkup.follow;

import com.linkup.common.ApiResponse;
import com.linkup.follow.dto.FollowDto;
import com.linkup.security.CurrentUser;
import com.linkup.user.dto.UserDto;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FollowController {
    private final FollowService followService;
    private final CurrentUser currentUser;

    public FollowController(FollowService followService, CurrentUser currentUser) {
        this.followService = followService;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/follows/{targetUserId}")
    ApiResponse<FollowDto> follow(@PathVariable Long targetUserId, Authentication authentication) {
        return ApiResponse.ok(followService.follow(currentUser.id(authentication), targetUserId));
    }

    @PostMapping("/api/follows/{followerId}/approve")
    ApiResponse<FollowDto> approve(@PathVariable Long followerId, Authentication authentication) {
        return ApiResponse.ok(followService.approve(followerId, currentUser.id(authentication)));
    }

    @PostMapping("/api/follows/{followerId}/decline")
    ApiResponse<Void> decline(@PathVariable Long followerId, Authentication authentication) {
        followService.decline(followerId, currentUser.id(authentication));
        return ApiResponse.message("Follow request declined");
    }

    @DeleteMapping("/api/follows/{targetUserId}")
    ApiResponse<Void> unfollow(@PathVariable Long targetUserId, Authentication authentication) {
        followService.unfollow(currentUser.id(authentication), targetUserId);
        return ApiResponse.message("Unfollowed successfully");
    }

    @GetMapping("/api/users/{userId}/followers")
    ApiResponse<List<UserDto>> followers(@PathVariable Long userId) {
        return ApiResponse.ok(followService.followers(userId));
    }

    @GetMapping("/api/users/{userId}/following")
    ApiResponse<List<UserDto>> following(@PathVariable Long userId) {
        return ApiResponse.ok(followService.following(userId));
    }

    @GetMapping("/api/follows/requests")
    ApiResponse<List<FollowDto>> pending(Authentication authentication) {
        return ApiResponse.ok(followService.pendingRequests(currentUser.id(authentication)));
    }

    @GetMapping("/api/users/{userId}/follow-status")
    ApiResponse<Map<String, String>> status(@PathVariable Long userId, Authentication authentication) {
        return ApiResponse.ok(Map.of("status", followService.status(currentUser.id(authentication), userId)));
    }
}
