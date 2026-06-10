package com.linkup.media;

import com.cloudinary.Cloudinary;
import com.linkup.common.BadRequestException;
import com.linkup.common.ForbiddenException;
import com.linkup.common.ResourceNotFoundException;
import com.linkup.media.dto.MediaDto;
import com.linkup.user.User;
import com.linkup.user.UserService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;
    private final MediaRepository mediaRepository;
    private final UserService userService;
    private final Cloudinary cloudinary;
    private final boolean cloudinaryConfigured;

    public MediaService(
            MediaRepository mediaRepository,
            UserService userService,
            Cloudinary cloudinary,
            @Value("${app.cloudinary.cloud-name}") String cloudName,
            @Value("${app.cloudinary.api-key}") String apiKey,
            @Value("${app.cloudinary.api-secret}") String apiSecret) {
        this.mediaRepository = mediaRepository;
        this.userService = userService;
        this.cloudinary = cloudinary;
        this.cloudinaryConfigured = hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    @Transactional
    public MediaDto uploadImage(MultipartFile file, Long userId) {
        validateImage(file);
        ensureCloudinaryConfigured();
        User user = userService.get(userId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), Map.of(
                    "folder", "linkup/images",
                    "resource_type", "image",
                    "quality", "auto",
                    "fetch_format", "auto",
                    "eager", List.of(Map.of("width", 480, "height", 480, "crop", "fill", "quality", "auto"))));
            String secureUrl = requireSecureUrl(result, "image");
            Media media = new Media();
            media.setUser(user);
            media.setUrl(secureUrl);
            media.setThumbnailUrl(extractThumbnailUrl(result));
            media.setProviderPublicId(String.valueOf(result.get("public_id")));
            media.setType("image");
            media.setOriginalFilename(file.getOriginalFilename());
            media.setFileSize(file.getSize());
            media.setWidth(toInteger(result.get("width")));
            media.setHeight(toInteger(result.get("height")));
            return toDto(mediaRepository.save(media));
        } catch (IOException ex) {
            throw new BadRequestException("Could not upload image");
        } catch (BadRequestException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BadRequestException("Cloudinary image upload failed");
        }
    }

    @Transactional
    public MediaDto uploadVideo(MultipartFile file, Long userId) {
        validateVideo(file);
        ensureCloudinaryConfigured();
        User user = userService.get(userId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), Map.of(
                    "folder", "linkup/videos",
                    "resource_type", "video"));
            String secureUrl = requireSecureUrl(result, "video");
            Media media = new Media();
            media.setUser(user);
            media.setUrl(secureUrl);
            media.setThumbnailUrl(extractVideoThumbnail(result));
            media.setProviderPublicId(String.valueOf(result.get("public_id")));
            media.setType("video");
            media.setOriginalFilename(file.getOriginalFilename());
            media.setFileSize(file.getSize());
            media.setWidth(toInteger(result.get("width")));
            media.setHeight(toInteger(result.get("height")));
            return toDto(mediaRepository.save(media));
        } catch (IOException ex) {
            throw new BadRequestException("Could not upload video");
        } catch (BadRequestException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BadRequestException("Cloudinary video upload failed");
        }
    }

    @Transactional
    public void delete(Long mediaId, Long userId) {
        Media media = mediaRepository.findById(mediaId).orElseThrow(() -> new ResourceNotFoundException("Media not found"));
        if (!media.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the media owner can delete this media");
        }
        deleteFromCloudinary(media);
        mediaRepository.delete(media);
    }

    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > MAX_VIDEO_BYTES) {
            throw new BadRequestException("Video must be 50MB or smaller");
        }
        String type = file.getContentType() == null ? "" : file.getContentType();
        if (!type.startsWith("video/")) {
            throw new BadRequestException("Only video files are supported");
        }
    }

    private String extractVideoThumbnail(Map<String, Object> result) {
        String url = String.valueOf(result.get("secure_url"));
        if (url != null && url.contains(".")) {
            return url.substring(0, url.lastIndexOf('.')) + ".jpg";
        }
        return null;
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
        return new MediaDto(media.getId(), media.getUrl(), media.getThumbnailUrl(), media.getType(), media.getPosition(), media.getOriginalFilename(), media.getFileSize(), media.getCreatedAt());
    }

    private String extractThumbnailUrl(Map<String, Object> result) {
        Object eager = result.get("eager");
        if (eager instanceof List<?> values && !values.isEmpty() && values.get(0) instanceof Map<?, ?> first) {
            Object secureUrl = first.get("secure_url");
            return secureUrl == null ? null : String.valueOf(secureUrl);
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private void ensureCloudinaryConfigured() {
        if (!cloudinaryConfigured) {
            throw new BadRequestException("Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET.");
        }
    }

    private String requireSecureUrl(Map<String, Object> result, String type) {
        Object secureUrl = result.get("secure_url");
        if (secureUrl == null || String.valueOf(secureUrl).isBlank()) {
            throw new BadRequestException("Cloudinary did not return a " + type + " URL");
        }
        return String.valueOf(secureUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void deleteFromCloudinary(Media media) {
        if (!cloudinaryConfigured || !hasText(media.getProviderPublicId())) {
            return;
        }
        try {
            cloudinary.uploader().destroy(media.getProviderPublicId(), Map.of("resource_type", media.getType()));
        } catch (IOException | RuntimeException ignored) {
            // Database cleanup should still succeed even if remote cleanup fails.
        }
    }
}

