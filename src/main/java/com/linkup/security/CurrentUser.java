package com.linkup.security;

import com.linkup.user.User;
import com.linkup.user.UserRepository;
import org.springframework.security.core.Authentication;
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

    public User user(Authentication authentication) {
        return userRepository.findById(id(authentication)).orElseThrow();
    }
}
