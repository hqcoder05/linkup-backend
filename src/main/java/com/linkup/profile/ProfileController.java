package com.linkup.profile;

import com.linkup.common.ApiResponse;
import com.linkup.profile.dto.ProfileDto;
import com.linkup.profile.dto.UpdateProfileRequest;
import com.linkup.security.CurrentUser;
import com.linkup.user.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final UserService userService;
    private final CurrentUser currentUser;

    public ProfileController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    ApiResponse<ProfileDto> me(Authentication authentication) {
        return ApiResponse.ok(userService.profile(currentUser.id(authentication)));
    }

    @GetMapping("/{userId}")
    ApiResponse<ProfileDto> byUser(@PathVariable Long userId) {
        return ApiResponse.ok(userService.profile(userId));
    }

    @PutMapping("/me")
    ApiResponse<ProfileDto> update(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
        return ApiResponse.ok(userService.updateProfile(currentUser.id(authentication), request));
    }
}
