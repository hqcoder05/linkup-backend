package com.linkup.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class HashtagServiceTest {
    @Test
    void ranksFreshVelocityAboveOlderHigherUsage() {
        HashtagRepository hashtagRepository = org.mockito.Mockito.mock(HashtagRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-08T12:00:00Z"), ZoneOffset.UTC);
        HashtagService hashtagService = new HashtagService(hashtagRepository, clock);

        when(hashtagRepository.findTrendingCandidates(PageRequest.of(0, 20))).thenReturn(List.of(
                candidate("oldtech", 20, Instant.parse("2026-06-07T13:00:00Z")),
                candidate("java", 10, Instant.parse("2026-06-08T10:00:00Z")),
                candidate("spring", 5, Instant.parse("2026-06-08T10:30:00Z"))
        ));

        var trending = hashtagService.trending(20);

        assertThat(trending).extracting("name").containsExactly("java", "spring", "oldtech");
        assertThat(trending.getFirst().trendScore()).isGreaterThan(trending.getLast().trendScore());
    }

    @Test
    void returnsEmptyListWhenThereAreNoCandidates() {
        HashtagRepository hashtagRepository = org.mockito.Mockito.mock(HashtagRepository.class);
        HashtagService hashtagService = new HashtagService(hashtagRepository, Clock.systemUTC());

        when(hashtagRepository.findTrendingCandidates(PageRequest.of(0, 20))).thenReturn(List.of());

        assertThat(hashtagService.trending(20)).isEmpty();
    }

    private HashtagRepository.TrendingHashtagProjection candidate(String name, long usageCount, Instant lastUsedAt) {
        return new HashtagRepository.TrendingHashtagProjection() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public long getUsageCount() {
                return usageCount;
            }

            @Override
            public Instant getLastUsedAt() {
                return lastUsedAt;
            }
        };
    }
}
