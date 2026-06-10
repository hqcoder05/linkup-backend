package com.linkup.settings;

import com.linkup.auth.RefreshToken;
import com.linkup.auth.RefreshTokenRepository;
import com.linkup.common.BadRequestException;
import com.linkup.common.ResourceNotFoundException;
import com.linkup.settings.dto.SettingsDtos.AccountSettingsDto;
import com.linkup.settings.dto.SettingsDtos.ChangePasswordRequest;
import com.linkup.settings.dto.SettingsDtos.SessionDto;
import com.linkup.settings.dto.SettingsDtos.UpdateAccountSettingsRequest;
import com.linkup.user.User;
import com.linkup.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingsService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AccountSettingsDto get(Long userId) {
        return toDto(user(userId));
    }

    @Transactional
    public AccountSettingsDto update(Long userId, UpdateAccountSettingsRequest request) {
        User user = user(userId);
        user.setPhoneNumber(blankToNull(request.phoneNumber()));
        user.setDateOfBirth(request.dateOfBirth());
        if (request.emailNotificationsEnabled() != null) {
            user.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
        }
        if (request.pushNotificationsEnabled() != null) {
            user.setPushNotificationsEnabled(request.pushNotificationsEnabled());
        }
        if (request.autoplayVideoEnabled() != null) {
            user.setAutoplayVideoEnabled(request.autoplayVideoEnabled());
        }
        if (request.contentVisibleToPublic() != null) {
            user.setContentVisibleToPublic(request.contentVisibleToPublic());
        }
        if (request.searchIndexingEnabled() != null) {
            user.setSearchIndexingEnabled(request.searchIndexingEnabled());
        }
        if (request.twoFactorEnabled() != null) {
            user.setTwoFactorEnabled(request.twoFactorEnabled());
        }
        return toDto(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = user(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<SessionDto> sessions(Long userId, String currentRefreshToken) {
        Long currentTokenId = null;
        if (currentRefreshToken != null && !currentRefreshToken.isBlank()) {
            currentTokenId = refreshTokenRepository.findByToken(currentRefreshToken)
                    .filter(token -> token.getUser().getId().equals(userId))
                    .map(RefreshToken::getId)
                    .orElse(null);
        }
        Long activeCurrentTokenId = currentTokenId;
        return refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(userId, Instant.now()).stream()
                .map(token -> new SessionDto(token.getId(), token.getCreatedAt(), token.getExpiresAt(), token.getId().equals(activeCurrentTokenId)))
                .toList();
    }

    @Transactional
    public void revokeSession(Long userId, Long tokenId) {
        refreshTokenRepository.revokeForUser(userId, tokenId);
    }

    @Transactional
    public void deactivate(Long userId) {
        User user = user(userId);
        user.setActive(false);
        user.setDeactivatedAt(Instant.now());
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private User user(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AccountSettingsDto toDto(User user) {
        return new AccountSettingsDto(
                user.getPhoneNumber(),
                user.getDateOfBirth(),
                user.isEmailNotificationsEnabled(),
                user.isPushNotificationsEnabled(),
                user.isAutoplayVideoEnabled(),
                user.isContentVisibleToPublic(),
                user.isSearchIndexingEnabled(),
                user.isTwoFactorEnabled(),
                user.isActive(),
                user.getDeactivatedAt());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
