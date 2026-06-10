package com.linkup.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.linkup.chat.MessageRepository;
import com.linkup.comment.CommentRepository;
import com.linkup.like.LikeRepository;
import com.linkup.user.User;
import com.linkup.user.UserRole;
import com.linkup.user.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {
    @Mock
    private StoryRepository storyRepository;
    @Mock
    private StoryViewRepository storyViewRepository;
    @Mock
    private UserService userService;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private MessageRepository messageRepository;
    @InjectMocks
    private StoryService storyService;

    @Test
    void visibleStoriesRanksUnseenByAffinityThenMovesSeenBubbleBehindUnseen() {
        Story storyA = story(101L, user(2L, "User A"), Instant.parse("2026-06-08T01:00:00Z"));
        Story storyB = story(102L, user(3L, "User B"), Instant.parse("2026-06-08T02:00:00Z"));

        when(storyRepository.findVisibleStories(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(storyA, storyB));
        when(messageRepository.countMessagesBetween(1L, 3L)).thenReturn(20L);
        when(messageRepository.countMessagesBetween(1L, 2L)).thenReturn(0L);

        when(storyViewRepository.findSeenStoryIds(1L)).thenReturn(List.of());
        assertThat(storyService.visibleStories(1L)).extracting(dto -> dto.user().id()).containsExactly(3L, 2L);

        when(storyViewRepository.findSeenStoryIds(1L)).thenReturn(List.of(102L));
        assertThat(storyService.visibleStories(1L)).extracting(dto -> dto.user().id()).containsExactly(2L, 3L);
    }

    private Story story(Long id, User user, Instant createdAt) {
        Story story = new Story();
        story.setId(id);
        story.setUser(user);
        story.setCreatedAt(createdAt);
        story.setExpiresAt(createdAt.plusSeconds(24 * 60 * 60));
        story.setActive(true);
        return story;
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(name.toLowerCase().replace(" ", ".") + "@example.com");
        user.setFullName(name);
        user.setRole(UserRole.USER);
        return user;
    }
}
