package com.linkup.post;

import java.time.Duration;
import java.time.Instant;

final class FeedRanking {
    private FeedRanking() {
    }

    static double hotScore(int likesCount, int commentsCount, Instant createdAt, Instant now) {
        double hours = Math.max(0.0, Duration.between(createdAt, now).toMinutes() / 60.0);
        return (likesCount + commentsCount * 2.0) / Math.pow(hours + 2.0, 1.5);
    }
}
