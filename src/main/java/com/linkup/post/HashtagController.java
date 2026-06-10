package com.linkup.post;

import com.linkup.common.ApiResponse;
import com.linkup.post.dto.TrendingHashtagDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HashtagController {
    private final HashtagService hashtagService;

    public HashtagController(HashtagService hashtagService) {
        this.hashtagService = hashtagService;
    }

    @GetMapping("/api/hashtags/trending")
    ApiResponse<List<TrendingHashtagDto>> trending(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(hashtagService.trending(limit));
    }
}
