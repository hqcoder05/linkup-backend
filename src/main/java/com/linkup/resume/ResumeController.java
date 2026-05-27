package com.linkup.resume;

import com.linkup.common.ApiResponse;
import com.linkup.resume.dto.ResumeDto;
import com.linkup.security.CurrentUser;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {
    private final ResumeService resumeService;
    private final CurrentUser currentUser;

    public ResumeController(ResumeService resumeService, CurrentUser currentUser) {
        this.resumeService = resumeService;
        this.currentUser = currentUser;
    }

    @PostMapping("/upload")
    ApiResponse<ResumeDto> upload(@RequestParam MultipartFile file, Authentication authentication) {
        return ApiResponse.ok(resumeService.upload(file, currentUser.id(authentication)));
    }

    @GetMapping("/me")
    ApiResponse<List<ResumeDto>> mine(Authentication authentication) {
        return ApiResponse.ok(resumeService.mine(currentUser.id(authentication)));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        resumeService.delete(id, currentUser.id(authentication));
        return ApiResponse.message("Resume deleted successfully");
    }
}
