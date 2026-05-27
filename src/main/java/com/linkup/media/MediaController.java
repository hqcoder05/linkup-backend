package com.linkup.media;

import com.linkup.common.ApiResponse;
import com.linkup.media.dto.MediaDto;
import com.linkup.security.CurrentUser;
import com.linkup.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final MediaService mediaService;
    private final UserService userService;
    private final CurrentUser currentUser;

    public MediaController(MediaService mediaService, UserService userService, CurrentUser currentUser) {
        this.mediaService = mediaService;
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @PostMapping("/images")
    ApiResponse<MediaDto> uploadImage(@RequestParam MultipartFile file, Authentication authentication) {
        return ApiResponse.ok(mediaService.uploadImage(file, currentUser.id(authentication)));
    }

    @PostMapping("/avatar")
    ApiResponse<MediaDto> updateAvatar(@RequestParam MultipartFile file, Authentication authentication) {
        MediaDto media = mediaService.uploadImage(file, currentUser.id(authentication));
        userService.updateAvatar(currentUser.id(authentication), media.url());
        return ApiResponse.ok(media);
    }
}
