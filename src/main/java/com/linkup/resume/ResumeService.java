package com.linkup.resume;

import com.linkup.common.BadRequestException;
import com.linkup.common.ForbiddenException;
import com.linkup.common.ResourceNotFoundException;
import com.linkup.resume.dto.ResumeDto;
import com.linkup.user.User;
import com.linkup.user.UserService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeService {
    private static final long MAX_RESUME_BYTES = 10L * 1024 * 1024;
    private final ResumeRepository resumeRepository;
    private final UserService userService;
    private final String resumesBaseUrl;

    public ResumeService(ResumeRepository resumeRepository, UserService userService, @Value("${app.storage.resumes-base-url}") String resumesBaseUrl) {
        this.resumeRepository = resumeRepository;
        this.userService = userService;
        this.resumesBaseUrl = resumesBaseUrl;
    }

    @Transactional
    public ResumeDto upload(MultipartFile file, Long userId) {
        validateResume(file);
        User user = userService.get(userId);
        String filename = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename();
        String url = (resumesBaseUrl == null || resumesBaseUrl.isBlank())
                ? "external-storage-not-configured://" + UUID.randomUUID() + "/" + filename
                : resumesBaseUrl.replaceAll("/$", "") + "/" + UUID.randomUUID() + "-" + filename;
        Resume resume = new Resume();
        resume.setUser(user);
        resume.setUrl(url);
        resume.setOriginalFilename(filename);
        resume.setContentType(file.getContentType());
        resume.setFileSize(file.getSize());
        return toDto(resumeRepository.save(resume));
    }

    public List<ResumeDto> mine(Long userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Resume resume = resumeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        if (!resume.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the owner can delete this resume");
        }
        resumeRepository.delete(resume);
    }

    private void validateResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > MAX_RESUME_BYTES) {
            throw new BadRequestException("Resume must be 10MB or smaller");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".pdf") && !name.endsWith(".doc") && !name.endsWith(".docx")) {
            throw new BadRequestException("Only PDF, DOC, and DOCX resumes are supported");
        }
    }

    private ResumeDto toDto(Resume resume) {
        return new ResumeDto(resume.getId(), resume.getUrl(), resume.getOriginalFilename(), resume.getContentType(), resume.getFileSize(), resume.getCreatedAt());
    }
}
