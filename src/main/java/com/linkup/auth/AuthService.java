package com.linkup.auth;

import com.linkup.auth.dto.AuthResponse;
import com.linkup.auth.dto.LoginRequest;
import com.linkup.auth.dto.RefreshTokenRequest;
import com.linkup.auth.dto.RegisterRequest;
import com.linkup.common.BadRequestException;
import com.linkup.profile.Profile;
import com.linkup.profile.ProfileRepository;
import com.linkup.security.JwtService;
import com.linkup.user.User;
import com.linkup.user.UserMapper;
import com.linkup.user.UserRepository;
import com.linkup.user.UserRole;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshTokenDays;

    public AuthService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            @Value("${app.jwt.refresh-token-days}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email already exists");
        }
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user = userRepository.save(user);

        Profile profile = new Profile();
        profile.setUser(user);
        profileRepository.save(profile);

        return tokensFor(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        return tokensFor(user);
    }

    public AuthResponse me(User user) {
        return new AuthResponse(null, null, UserMapper.toDto(user));
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken current = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (current.isRevoked() || current.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid refresh token");
        }
        if (!current.getUser().isActive()) {
            throw new BadRequestException("Account is deactivated");
        }
        current.setRevoked(true);
        return tokensFor(current.getUser());
    }

    private AuthResponse tokensFor(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateRefreshToken());
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshTokenDays * 24 * 60 * 60));
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(jwtService.generateAccessToken(user), refreshToken.getToken(), UserMapper.toDto(user));
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
