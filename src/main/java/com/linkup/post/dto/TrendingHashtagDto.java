package com.linkup.post.dto;

public record TrendingHashtagDto(
        String name,
        long usageCount,
        double trendScore
) {
}
