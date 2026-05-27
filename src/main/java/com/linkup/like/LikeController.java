package com.linkup.like;

import com.linkup.common.ApiResponse;
import com.linkup.security.CurrentUser;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LikeController {
    private final LikeService likeService;
    private final CurrentUser currentUser;

    public LikeController(LikeService likeService, CurrentUser currentUser) {
        this.likeService = likeService;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/posts/{postId}/likes")
    ApiResponse<Map<String, Object>> like(@PathVariable Long postId, Authentication authentication) {
        long count = likeService.likePost(postId, currentUser.id(authentication));
        return ApiResponse.ok(Map.of("liked", true, "likesCount", count));
    }

    @DeleteMapping("/api/posts/{postId}/likes")
    ApiResponse<Map<String, Object>> unlike(@PathVariable Long postId, Authentication authentication) {
        long count = likeService.unlikePost(postId, currentUser.id(authentication));
        return ApiResponse.ok(Map.of("liked", false, "likesCount", count));
    }
}
