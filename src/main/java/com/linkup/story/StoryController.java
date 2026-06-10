package com.linkup.story;

import com.linkup.common.ApiResponse;
import com.linkup.security.CurrentUser;
import com.linkup.story.dto.StoryDtos.CreateStoryRequest;
import com.linkup.story.dto.StoryDtos.StoryDto;
import com.linkup.story.dto.StoryDtos.StoryViewerDto;
import com.linkup.story.dto.StoryDtos.UserStoriesDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stories")
public class StoryController {
    private final StoryService storyService;
    private final CurrentUser currentUser;

    public StoryController(StoryService storyService, CurrentUser currentUser) {
        this.storyService = storyService;
        this.currentUser = currentUser;
    }

    @PostMapping
    ApiResponse<StoryDto> create(@Valid @RequestBody CreateStoryRequest request, Authentication authentication) {
        return ApiResponse.ok(storyService.create(currentUser.id(authentication), request));
    }

    @GetMapping
    ApiResponse<List<UserStoriesDto>> stories(Authentication authentication) {
        return ApiResponse.ok(storyService.visibleStories(currentUser.id(authentication)));
    }

    @GetMapping("/{storyId}")
    ApiResponse<StoryDto> detail(@PathVariable Long storyId, Authentication authentication) {
        return ApiResponse.ok(storyService.detail(storyId, currentUser.id(authentication)));
    }

    @DeleteMapping("/{storyId}")
    ApiResponse<Void> delete(@PathVariable Long storyId, Authentication authentication) {
        storyService.delete(storyId, currentUser.id(authentication));
        return ApiResponse.message("Story deleted");
    }

    @GetMapping("/{storyId}/viewers")
    ApiResponse<List<StoryViewerDto>> viewers(@PathVariable Long storyId, Authentication authentication) {
        return ApiResponse.ok(storyService.viewers(storyId, currentUser.id(authentication)));
    }

    @PostMapping("/{storyId}/seen")
    ApiResponse<Void> seen(@PathVariable Long storyId, Authentication authentication) {
        storyService.markAsSeen(storyId, currentUser.id(authentication));
        return ApiResponse.message("Story marked as seen");
    }
}
