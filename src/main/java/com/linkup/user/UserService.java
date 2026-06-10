package com.linkup.user;

import com.linkup.common.ResourceNotFoundException;
import com.linkup.follow.FollowRepository;
import com.linkup.follow.FollowStatus;
import com.linkup.post.PostRepository;
import com.linkup.profile.Profile;
import com.linkup.profile.ProfileRepository;
import com.linkup.profile.dto.ProfileDto;
import com.linkup.profile.dto.UpdateProfileRequest;
import com.linkup.user.dto.UserDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;

    public UserService(UserRepository userRepository, ProfileRepository profileRepository, PostRepository postRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.postRepository = postRepository;
        this.followRepository = followRepository;
    }

    public User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));  
    }

    @Transactional(readOnly = true)
    public List<UserDto> search(String keyword, Long currentUserId) {
        String value = sanitizeKeyword(keyword);
        if (value.isBlank()) {
            return List.of();
        }
        String keywordPrefix = escapeLike(value) + "%";
        String keywordWildcard = "%" + escapeLike(value) + "%";
        return userRepository.searchRanked(currentUserId, keywordPrefix, keywordWildcard, value)
                .stream()
                .map(user -> UserMapper.toDto(user, followRepository.existsByIdFollowerIdAndIdFollowingIdAndStatus(
                        currentUserId,
                        user.getId(),
                        FollowStatus.ACCEPTED)))
                .toList();
    }

    @Transactional
    public ProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = get(userId);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.privateAccount() != null) {
            user.setPrivateAccount(request.privateAccount());
        }
        if (request.avatarUrl() != null && !request.avatarUrl().isBlank()) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.coverUrl() != null && !request.coverUrl().isBlank()) {
            user.setCoverUrl(request.coverUrl());
        }
        Profile profile = profileRepository.findByUserId(userId).orElseGet(() -> {
            Profile created = new Profile();
            created.setUser(user);
            return created;
        });
        profile.setNickname(request.nickname());
        profile.setBio(request.bio());
        profile.setHeadline(request.headline());
        profile.setLocation(request.location());
        profile.setWebsiteUrl(request.websiteUrl());
        return toProfileDto(profileRepository.save(profile));
    }

    @Transactional
    public UserDto updateCover(Long userId, String coverUrl) {
        User user = get(userId);
        user.setCoverUrl(coverUrl);
        return UserMapper.toDto(user);
    }

    @Transactional
    public UserDto updateAvatar(Long userId, String avatarUrl) {
        User user = get(userId);
        user.setAvatarUrl(avatarUrl);
        return UserMapper.toDto(user);
    }

    @Transactional
    public ProfileDto profile(Long userId) {
        User user = get(userId);
        Profile profile = profileRepository.findByUserId(userId).orElseGet(() -> {
            Profile created = new Profile();
            created.setUser(user);
            return profileRepository.save(created);
        });
        return toProfileDto(profile);
    }

    private ProfileDto toProfileDto(Profile profile) {
        return new ProfileDto(
                profile.getId(),
                UserMapper.toDto(profile.getUser()),
                profile.getNickname(),
                profile.getBio(),
                profile.getHeadline(),
                profile.getLocation(),
                profile.getWebsiteUrl(),
                postRepository.countByUserId(profile.getUser().getId()),
                followRepository.countByIdFollowingIdAndStatus(profile.getUser().getId(), FollowStatus.ACCEPTED),
                followRepository.countByIdFollowerIdAndStatus(profile.getUser().getId(), FollowStatus.ACCEPTED),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    private String sanitizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        return keyword.trim().replaceAll("\\s+", " ");
    }

    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
