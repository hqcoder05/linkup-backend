package com.linkup.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.linkup.follow.FollowRepository;
import com.linkup.follow.FollowStatus;
import com.linkup.post.PostRepository;
import com.linkup.profile.ProfileRepository;
import com.linkup.user.dto.UserDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceSearchTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private FollowRepository followRepository;
    @InjectMocks
    private UserService userService;

    @Test
    void searchPreservesRankedRepositoryOrderAndMarksFollowingUsers() {
        User followed = user(2L, "John B");
        User stranger = user(3L, "John C");

        when(userRepository.searchRanked(1L, "John%", "%John%", "John")).thenReturn(List.of(followed, stranger));
        when(followRepository.existsByIdFollowerIdAndIdFollowingIdAndStatus(1L, 2L, FollowStatus.ACCEPTED)).thenReturn(true);
        when(followRepository.existsByIdFollowerIdAndIdFollowingIdAndStatus(1L, 3L, FollowStatus.ACCEPTED)).thenReturn(false);

        List<UserDto> results = userService.search(" John ", 1L);

        assertThat(results).extracting(UserDto::id).containsExactly(2L, 3L);
        assertThat(results.get(0).following()).isTrue();
        assertThat(results.get(1).following()).isFalse();
    }

    private User user(Long id, String fullName) {
        User user = new User();
        user.setId(id);
        user.setEmail(fullName.toLowerCase().replace(" ", ".") + "@example.com");
        user.setFullName(fullName);
        user.setRole(UserRole.USER);
        return user;
    }
}
