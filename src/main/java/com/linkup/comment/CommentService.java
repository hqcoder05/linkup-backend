package com.linkup.comment;

import com.linkup.comment.dto.CommentDtos.CommentDto;
import com.linkup.comment.dto.CommentDtos.CommentRequest;
import com.linkup.common.ForbiddenException;
import com.linkup.common.ResourceNotFoundException;
import com.linkup.notification.NotificationService;
import com.linkup.post.Post;
import com.linkup.post.PostRepository;
import com.linkup.post.PostService;
import com.linkup.user.User;
import com.linkup.user.UserMapper;
import com.linkup.user.UserService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostService postService;
    private final PostRepository postRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, PostService postService, PostRepository postRepository, UserService userService, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.postRepository = postRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional
    public CommentDto create(Long postId, Long userId, CommentRequest request) {
        Post post = postService.get(postId);
        User user = userService.get(userId);
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(request.content());
        Comment saved = commentRepository.saveAndFlush(comment);
        postRepository.incrementCommentsCount(postId);
        if (!post.getUser().getId().equals(userId)) {
            notificationService.create(post.getUser().getId(), "post_comment", "New comment", request.content(), "/posts/" + postId, String.valueOf(postId), userId);
        }
        return toDto(saved);
    }

    @Transactional
    public CommentDto update(Long commentId, Long userId, CommentRequest request) {
        Comment comment = get(commentId);
        if (!comment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the comment owner can change this comment");
        }
        comment.setContent(request.content());
        return toDto(comment);
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment comment = get(commentId);
        if (!comment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the comment owner can delete this comment");
        }
        Long postId = comment.getPost().getId();
        commentRepository.delete(comment);
        postRepository.decrementCommentsCount(postId);
    }

    public List<CommentDto> forPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream().map(this::toDto).toList();
    }

    private Comment get(Long id) {
        return commentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private CommentDto toDto(Comment comment) {
        return new CommentDto(comment.getId(), UserMapper.toDto(comment.getUser()), comment.getContent(), 0, false, comment.getCreatedAt());
    }
}
