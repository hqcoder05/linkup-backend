package com.linkup.post;

import com.linkup.common.ApiResponse;
import com.linkup.post.dto.PostDtos.CreatePostRequest;
import com.linkup.post.dto.PostDtos.PostDto;
import com.linkup.post.dto.PostDtos.UpdatePostRequest;
import com.linkup.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostController {
    private final PostService postService;
    private final FeedService feedService;
    private final CurrentUser currentUser;

    public PostController(PostService postService, FeedService feedService, CurrentUser currentUser) {
        this.postService = postService;
        this.feedService = feedService;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/posts")
    ApiResponse<PostDto> create(@Valid @RequestBody CreatePostRequest request, Authentication authentication) {
        return ApiResponse.ok(postService.create(currentUser.id(authentication), request));
    }

    @GetMapping("/api/posts/feed")
    ApiResponse<List<PostDto>> feed(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        return ApiResponse.ok(feedService.homeFeed(currentUser.id(authentication), page, size));
    }

    @GetMapping("/api/posts/explore")
    ApiResponse<List<PostDto>> explore(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        return ApiResponse.ok(feedService.explore(currentUser.id(authentication), page, size));
    }

    @GetMapping("/api/posts/{id}")
    ApiResponse<PostDto> get(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok(postService.getDto(id, currentUser.idOrNull(authentication)));
    }

    @GetMapping("/api/posts/search")
    ApiResponse<List<PostDto>> search(@RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        return ApiResponse.ok(postService.search(keyword, currentUser.idOrNull(authentication), page, size));
    }

    @GetMapping("/api/hashtags/{name}/posts")
    ApiResponse<List<PostDto>> byHashtag(@PathVariable String name, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        return ApiResponse.ok(postService.byHashtag(name, currentUser.idOrNull(authentication), page, size));
    }

    @PostMapping("/api/posts/{id}/save")
    ApiResponse<Void> save(@PathVariable Long id, Authentication authentication) {
        postService.savePost(id, currentUser.id(authentication));
        return ApiResponse.message("Post saved");
    }

    @DeleteMapping("/api/posts/{id}/save")
    ApiResponse<Void> unsave(@PathVariable Long id, Authentication authentication) {
        postService.unsavePost(id, currentUser.id(authentication));
        return ApiResponse.message("Post unsaved");
    }

    @GetMapping("/api/posts/saved")
    ApiResponse<List<PostDto>> saved(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        return ApiResponse.ok(postService.savedPosts(currentUser.id(authentication), page, size));
    }

    @PutMapping("/api/posts/{id}")
    ApiResponse<PostDto> update(@PathVariable Long id, @Valid @RequestBody UpdatePostRequest request, Authentication authentication) {
        return ApiResponse.ok(postService.update(id, currentUser.id(authentication), request));
    }

    @DeleteMapping("/api/posts/{id}")
    ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        postService.delete(id, currentUser.id(authentication));
        return ApiResponse.message("Post deleted successfully");
    }

    @GetMapping("/api/users/{userId}/posts")
    ApiResponse<List<PostDto>> byUser(@PathVariable Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        return ApiResponse.ok(postService.byUser(userId, currentUser.idOrNull(authentication), page, size));
    }
}
