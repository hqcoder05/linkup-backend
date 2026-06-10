package com.linkup.post;

import com.linkup.post.dto.TrendingHashtagDto;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HashtagService {
    private static final double GRAVITY = 1.5;
    private static final double OFFSET_HOURS = 2.0;

    private final HashtagRepository hashtagRepository;
    private final Clock clock;

    @Autowired
    public HashtagService(HashtagRepository hashtagRepository) {
        this(hashtagRepository, Clock.systemUTC());
    }

    HashtagService(HashtagRepository hashtagRepository, Clock clock) {
        this.hashtagRepository = hashtagRepository;
        this.clock = clock;
    }

    @Cacheable(value = "trendingHashtags", key = "#limit")
    public List<TrendingHashtagDto> trending(int limit) {
        int safeLimit = Math.clamp(limit, 1, 50);
        return hashtagRepository.findTrendingCandidates(PageRequest.of(0, Math.max(safeLimit, 20))).stream()
                .map(this::toTrendingDto)
                .sorted(Comparator.comparingDouble(TrendingHashtagDto::trendScore).reversed()
                        .thenComparing(TrendingHashtagDto::name))
                .limit(safeLimit)
                .toList();
    }

    private TrendingHashtagDto toTrendingDto(HashtagRepository.TrendingHashtagProjection projection) {
        double hoursSinceLastUse = Math.max(0.0, Duration.between(projection.getLastUsedAt(), Instant.now(clock)).toMinutes() / 60.0);
        double score = projection.getUsageCount() / Math.pow(hoursSinceLastUse + OFFSET_HOURS, GRAVITY);
        return new TrendingHashtagDto(projection.getName(), projection.getUsageCount(), score);
    }
}
