package com.linkup.comment;

import com.linkup.comment.dto.CommentDtos.CommentDto;
import com.linkup.comment.dto.CommentDtos.CommentRequest;
import com.linkup.common.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {
    private final CommentService commentService;
    private final CurrentUser currentUser;

    public CommentController(CommentService commentService, CurrentUser currentUser) {
        this.commentService = commentService;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/posts/{postId}/comments")
    ApiResponse<List<CommentDto>> forPost(@PathVariable Long postId) {
        return ApiResponse.ok(commentService.forPost(postId));
    }

    @PostMapping("/api/posts/{postId}/comments")
    ApiResponse<CommentDto> create(@PathVariable Long postId, @Valid @RequestBody CommentRequest request, Authentication authentication) {
        return ApiResponse.ok(commentService.create(postId, currentUser.id(authentication), request));
    }

    @PutMapping("/api/comments/{commentId}")
    ApiResponse<CommentDto> update(@PathVariable Long commentId, @Valid @RequestBody CommentRequest request, Authentication authentication) {
        return ApiResponse.ok(commentService.update(commentId, currentUser.id(authentication), request));
    }

    @DeleteMapping("/api/comments/{commentId}")
    ApiResponse<Void> delete(@PathVariable Long commentId, Authentication authentication) {
        commentService.delete(commentId, currentUser.id(authentication));
        return ApiResponse.message("Comment deleted successfully");
    }
}
