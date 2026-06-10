package com.linkup.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linkup.notification.NotificationService;
import com.linkup.post.Post;
import com.linkup.post.PostRepository;
import com.linkup.post.PostService;
import com.linkup.user.User;
import com.linkup.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private PostService postService;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserService userService;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private LikeService likeService;

    @Test
    void likePostIncrementsDenormalizedCounterOnlyWhenLikeIsNew() {
        User author = new User();
        author.setId(10L);
        User liker = new User();
        liker.setId(20L);
        Post post = new Post();
        post.setId(1L);
        post.setUser(author);

        when(likeRepository.existsByPostIdAndUserId(1L, 20L)).thenReturn(false);
        when(postService.get(1L)).thenReturn(post);
        when(userService.get(20L)).thenReturn(liker);
        when(postRepository.findLikesCountById(1L)).thenReturn(1);

        long likesCount = likeService.likePost(1L, 20L);

        assertThat(likesCount).isEqualTo(1);
        verify(postRepository).incrementLikesCount(1L);
        verify(notificationService).create(10L, "post_like", "New like", "Someone liked your post.", "/posts/1", "1", 20L);
    }

    @Test
    void likePostDoesNotIncrementCounterForDuplicateLike() {
        when(likeRepository.existsByPostIdAndUserId(1L, 20L)).thenReturn(true);
        when(postRepository.findLikesCountById(1L)).thenReturn(3);

        long likesCount = likeService.likePost(1L, 20L);

        assertThat(likesCount).isEqualTo(3);
        verify(postRepository, never()).incrementLikesCount(1L);
    }
}
