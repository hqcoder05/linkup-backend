package com.linkup.post;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByName(String name);

    @Query(value = """
            select h.id as id,
                   h.name as name,
                   count(ph.post_id) as "usageCount",
                   max(p.created_at) as "lastUsedAt"
            from hashtags h
            join post_hashtags ph on h.id = ph.hashtag_id
            join posts p on ph.post_id = p.id
            where p.created_at >= now() - interval '24 hours'
            group by h.id, h.name
            order by count(ph.post_id) desc
            """, nativeQuery = true)
    List<TrendingHashtagProjection> findTrendingCandidates(Pageable pageable);

    interface TrendingHashtagProjection {
        Long getId();
        String getName();
        long getUsageCount();
        Instant getLastUsedAt();
    }
}
