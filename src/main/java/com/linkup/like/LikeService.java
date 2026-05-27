package com.linkup.like;

import com.linkup.notification.NotificationService;
import com.linkup.post.Post;
import com.linkup.post.PostService;
import com.linkup.user.User;
import com.linkup.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostService postService;
    private final UserService userService;
    private final NotificationService notificationService;

    public LikeService(LikeRepository likeRepository, PostService postService, UserService userService, NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.postService = postService;
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
            likeRepository.save(like);
            if (!post.getUser().getId().equals(userId)) {
                notificationService.create(post.getUser().getId(), "post_like", "New like", "Someone liked your post.", "/posts/" + postId);
            }
        }
        return likeRepository.countByPostId(postId);
    }

    @Transactional
    public long unlikePost(Long postId, Long userId) {
        likeRepository.deleteByPostIdAndUserId(postId, userId);
        return likeRepository.countByPostId(postId);
    }
}
