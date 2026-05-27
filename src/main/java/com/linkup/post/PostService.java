package com.linkup.post;

import com.linkup.common.ForbiddenException;
import com.linkup.common.ResourceNotFoundException;
import com.linkup.comment.CommentRepository;
import com.linkup.like.LikeRepository;
import com.linkup.post.dto.PostDtos.CreatePostRequest;
import com.linkup.post.dto.PostDtos.PostDto;
import com.linkup.post.dto.PostDtos.UpdatePostRequest;
import com.linkup.user.User;
import com.linkup.user.UserMapper;
import com.linkup.user.UserService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    public PostService(PostRepository postRepository, UserService userService, LikeRepository likeRepository, CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public PostDto create(Long userId, CreatePostRequest request) {
        User user = userService.get(userId);
        Post post = new Post();
        post.setUser(user);
        post.setCaption(request.caption() == null ? "" : request.caption());
        post.setImageUrl(request.imageUrl());
        post.setVideoUrl(request.videoUrl());
        return toDto(postRepository.save(post), userId);
    }

    @Transactional
    public PostDto update(Long postId, Long userId, UpdatePostRequest request) {
        Post post = get(postId);
        ensureOwner(post, userId);
        post.setCaption(request.caption() == null ? "" : request.caption());
        return toDto(post, userId);
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        Post post = get(postId);
        ensureOwner(post, userId);
        postRepository.delete(post);
    }

    public PostDto getDto(Long postId, Long currentUserId) {
        return toDto(get(postId), currentUserId);
    }

    public Post get(Long postId) {
        return postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    public List<PostDto> feed(Long userId, int page, int size) {
        return postRepository.findFeed(userId, PageRequest.of(page, size)).stream().map(p -> toDto(p, userId)).toList();
    }

    public List<PostDto> byUser(Long userId, Long currentUserId, int page, int size) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)).stream()
                .map(p -> toDto(p, currentUserId)).toList();
    }

    private void ensureOwner(Post post, Long userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the post owner can change this post");
        }
    }

    public PostDto toDto(Post post, Long currentUserId) {
        return new PostDto(
                post.getId(),
                UserMapper.toDto(post.getUser()),
                post.getCaption(),
                post.getImageUrl(),
                post.getVideoUrl(),
                likeRepository.countByPostId(post.getId()),
                commentRepository.countByPostId(post.getId()),
                currentUserId != null && likeRepository.existsByPostIdAndUserId(post.getId(), currentUserId),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
