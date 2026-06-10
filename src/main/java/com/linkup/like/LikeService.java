package com.linkup.like;

import com.linkup.notification.NotificationService;
import com.linkup.post.Post;
import com.linkup.post.PostRepository;
import com.linkup.post.PostService;
import com.linkup.user.User;
import com.linkup.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostService postService;
    private final PostRepository postRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public LikeService(LikeRepository likeRepository, PostService postService, PostRepository postRepository, UserService userService, NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.postService = postService;
        this.postRepository = postRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional
    public long likePost(Long postId, Long userId) {
        if (!likeRepository.existsByPostIdAndUserId(postId, userId)) {
            Post post = postService.get(postId);
            User user = userService.get(userId);
            Like like = new Like();
            like.setPost(post);
            like.setUser(user);
            likeRepository.saveAndFlush(like);
            postRepository.incrementLikesCount(postId);
            if (!post.getUser().getId().equals(userId)) {
                notificationService.create(post.getUser().getId(), "post_like", "New like", "Someone liked your post.", "/posts/" + postId, String.valueOf(postId), userId);
            }
        }
        return postRepository.findLikesCountById(postId);
    }

    @Transactional
    public long unlikePost(Long postId, Long userId) {
        likeRepository.findByPostIdAndUserId(postId, userId).ifPresent(like -> {
            Long ownerId = like.getPost().getUser().getId();
            likeRepository.delete(like);
            postRepository.decrementLikesCount(postId);
            if (!ownerId.equals(userId)) {
                notificationService.decrementInteraction(ownerId, "post_like", String.valueOf(postId), userId);
            }
        });
        return postRepository.findLikesCountById(postId);
    }
}
