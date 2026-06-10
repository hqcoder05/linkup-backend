package com.linkup.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.linkup.follow.FollowRepository;
import com.linkup.post.PostHashtagRepository;
import com.linkup.profile.Profile;
import com.linkup.profile.ProfileRepository;
import com.linkup.user.dto.SuggestionDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {
    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private PostHashtagRepository postHashtagRepository;
    @InjectMocks
    private SuggestionService suggestionService;

    @Test
    void suggestionsPrioritizeFriendOfFriendWithSharedLocationAndHashtag() {
        User candidate = user(3L, "User C");
        when(followRepository.findMutualFollowCandidates(1L)).thenReturn(List.of(candidateProjection(3L, 1)));
        when(userRepository.findAllByIdsUnordered(List.of(3L))).thenReturn(List.of(candidate));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile(user(1L, "User A"), "Hanoi")));
        when(profileRepository.findByUserIds(List.of(3L))).thenReturn(List.of(profile(candidate, "hanoi")));
        when(postHashtagRepository.findInterestHashtagsForUser(1L)).thenReturn(List.of("tech"));
        when(postHashtagRepository.countSharedHashtagsByCandidates(List.of(3L), List.of("tech")))
                .thenReturn(List.of(hashtagOverlap(3L, 1)));
        when(followRepository.findMutualFriendNames(1L, 3L)).thenReturn(List.of("User B"));

        List<SuggestionDto> suggestions = suggestionService.suggestions(1L, 10);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).user().id()).isEqualTo(3L);
        assertThat(suggestions.get(0).mutualCount()).isEqualTo(1);
        assertThat(suggestions.get(0).mutualFriendNames()).containsExactly("User B");
    }

    @Test
    void suggestionsNeverReturnCurrentUserEvenIfCandidateSourcesDo() {
        User currentUser = user(1L, "User A");
        User candidate = user(3L, "User C");
        when(followRepository.findMutualFollowCandidates(1L)).thenReturn(List.of(candidateProjection(1L, 99)));
        when(userRepository.findTrendingSuggestionFallback(1L, 50)).thenReturn(List.of(currentUser, candidate));
        when(userRepository.findAllByIdsUnordered(List.of(3L))).thenReturn(List.of(candidate));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(postHashtagRepository.findInterestHashtagsForUser(1L)).thenReturn(List.of());
        when(followRepository.findMutualFriendNames(1L, 3L)).thenReturn(List.of());

        List<SuggestionDto> suggestions = suggestionService.suggestions(1L, 10);

        assertThat(suggestions).extracting(suggestion -> suggestion.user().id()).containsExactly(3L);
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setEmail(name.toLowerCase().replace(" ", ".") + "@example.com");
        user.setRole(UserRole.USER);
        return user;
    }

    private Profile profile(User user, String location) {
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setLocation(location);
        return profile;
    }

    private FollowRepository.SuggestionCandidateProjection candidateProjection(Long userId, int mutualCount) {
        return new FollowRepository.SuggestionCandidateProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public int getMutualCount() {
                return mutualCount;
            }
        };
    }

    private PostHashtagRepository.UserHashtagOverlapProjection hashtagOverlap(Long userId, int sharedCount) {
        return new PostHashtagRepository.UserHashtagOverlapProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public int getSharedCount() {
                return sharedCount;
            }
        };
    }
}
