package com.linkup.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class FeedRankingTest {

    @Test
    void hotScoreRanksFreshModerateEngagementAboveOldHighEngagement() {
        Instant now = Instant.parse("2026-06-08T00:00:00Z");

        double freshPostScore = FeedRanking.hotScore(5, 0, now, now);
        double weekOldPostScore = FeedRanking.hotScore(100, 0, now.minus(7, ChronoUnit.DAYS), now);

        assertThat(freshPostScore).isGreaterThan(weekOldPostScore);
    }
}
