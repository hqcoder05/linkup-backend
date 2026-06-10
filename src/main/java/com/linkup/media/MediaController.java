package com.linkup.media;

import com.linkup.common.ApiResponse;
import com.linkup.media.dto.MediaDto;
import com.linkup.security.CurrentUser;
import com.linkup.user.UserService;
import com.linkup.common.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/videos")
    ApiResponse<MediaDto> uploadVideo(@RequestParam MultipartFile file, Authentication authentication) {
        return ApiResponse.ok(mediaService.uploadVideo(file, currentUser.id(authentication)));
    }

    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<MediaDto> updateCover(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication authentication) {
        MediaDto media = mediaService.uploadImage(firstFile(file, cover, image), currentUser.id(authentication));
        userService.updateCover(currentUser.id(authentication), media.url());
        return ApiResponse.ok(media);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<MediaDto> updateAvatar(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication authentication) {
        MediaDto media = mediaService.uploadImage(firstFile(file, avatar, image), currentUser.id(authentication));
        userService.updateAvatar(currentUser.id(authentication), media.url());
        return ApiResponse.ok(media);
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        mediaService.delete(id, currentUser.id(authentication));
        return ApiResponse.message("Media deleted");
    }

    private MultipartFile firstFile(MultipartFile... files) {
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                return file;
            }
        }
        throw new BadRequestException("Upload file is required. Send multipart/form-data with field name file.");
    }
}
