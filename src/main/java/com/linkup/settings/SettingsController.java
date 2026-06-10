package com.linkup.settings;

import com.linkup.common.ApiResponse;
import com.linkup.security.CurrentUser;
import com.linkup.settings.dto.SettingsDtos.AccountSettingsDto;
import com.linkup.settings.dto.SettingsDtos.ChangePasswordRequest;
import com.linkup.settings.dto.SettingsDtos.SessionDto;
import com.linkup.settings.dto.SettingsDtos.UpdateAccountSettingsRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settingsService;
    private final CurrentUser currentUser;

    public SettingsController(SettingsService settingsService, CurrentUser currentUser) {
        this.settingsService = settingsService;
        this.currentUser = currentUser;
    }

    @GetMapping
    ApiResponse<AccountSettingsDto> get(Authentication authentication) {
        return ApiResponse.ok(settingsService.get(currentUser.id(authentication)));
    }

    @PutMapping
    ApiResponse<AccountSettingsDto> update(@Valid @RequestBody UpdateAccountSettingsRequest request, Authentication authentication) {
        return ApiResponse.ok(settingsService.update(currentUser.id(authentication), request));
    }

    @PostMapping("/password")
    ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        settingsService.changePassword(currentUser.id(authentication), request);
        return ApiResponse.message("Password changed successfully");
    }

    @GetMapping("/sessions")
    ApiResponse<List<SessionDto>> sessions(@RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken, Authentication authentication) {
        return ApiResponse.ok(settingsService.sessions(currentUser.id(authentication), refreshToken));
    }

    @DeleteMapping("/sessions/{tokenId}")
    ApiResponse<Void> revokeSession(@PathVariable Long tokenId, Authentication authentication) {
        settingsService.revokeSession(currentUser.id(authentication), tokenId);
        return ApiResponse.message("Session revoked");
    }

    @DeleteMapping("/account")
    ApiResponse<Map<String, Boolean>> deactivate(Authentication authentication) {
        settingsService.deactivate(currentUser.id(authentication));
        return ApiResponse.ok("Account deactivated", Map.of("deactivated", true));
    }
}
