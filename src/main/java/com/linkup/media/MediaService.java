package com.linkup.media;

import com.cloudinary.Cloudinary;
import com.linkup.common.BadRequestException;
import com.linkup.media.dto.MediaDto;
import com.linkup.user.User;
import com.linkup.user.UserService;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private final MediaRepository mediaRepository;
    private final UserService userService;
    private final Cloudinary cloudinary;

    public MediaService(MediaRepository mediaRepository, UserService userService, Cloudinary cloudinary) {
        this.mediaRepository = mediaRepository;
        this.userService = userService;
        this.cloudinary = cloudinary;
    }

    @Transactional
    public MediaDto uploadImage(MultipartFile file, Long userId) {
        validateImage(file);
        User user = userService.get(userId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), Map.of("folder", "linkup/images", "resource_type", "image"));
            Media media = new Media();
            media.setUser(user);
            media.setUrl(String.valueOf(result.get("secure_url")));
            media.setProviderPublicId(String.valueOf(result.get("public_id")));
            media.setType("image");
            media.setOriginalFilename(file.getOriginalFilename());
            media.setFileSize(file.getSize());
            return toDto(mediaRepository.save(media));
        } catch (IOException ex) {
            throw new BadRequestException("Could not upload image");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Image must be 5MB or smaller");
        }
        String type = file.getContentType() == null ? "" : file.getContentType();
        if (!type.equals("image/jpeg") && !type.equals("image/png") && !type.equals("image/webp") && !type.equals("image/gif")) {
            throw new BadRequestException("Only JPG, PNG, WEBP, or GIF images are supported");
        }
    }

    private MediaDto toDto(Media media) {
        return new MediaDto(media.getId(), media.getUrl(), media.getType(), media.getOriginalFilename(), media.getFileSize(), media.getCreatedAt());
    }
}
