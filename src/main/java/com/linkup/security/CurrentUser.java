package com.linkup.security;

import com.linkup.user.User;
import com.linkup.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long id(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    public Long idOrNull(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return null;
        }
        return Long.valueOf(authentication.getName());
    }

    public User user(Authentication authentication) {
        User user = userRepository.findById(id(authentication)).orElseThrow();
        if (!user.isActive()) {
            throw new org.springframework.security.access.AccessDeniedException("Account is deactivated");
        }
        return user;
    }
}
