package com.linkup.user;

import com.linkup.common.ResourceNotFoundException;
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

    public UserService(UserRepository userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    public User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<UserDto> search(String keyword, Long currentUserId) {
        String value = keyword == null ? "" : keyword.trim();
        return userRepository.findTop20ByIdNotAndFullNameContainingIgnoreCaseOrIdNotAndEmailContainingIgnoreCase(
                        currentUserId, value, currentUserId, value)
                .stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Transactional
    public ProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = get(userId);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
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
    public UserDto updateAvatar(Long userId, String avatarUrl) {
        User user = get(userId);
        user.setAvatarUrl(avatarUrl);
        return UserMapper.toDto(user);
    }

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
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
