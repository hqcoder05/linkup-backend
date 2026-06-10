package com.linkup.story;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface StoryRepository extends JpaRepository<Story, Long> {
    @EntityGraph(attributePaths = {"user", "media"})
    @Query("select s from Story s where s.id = :id")
    java.util.Optional<Story> findWithUserAndMediaById(Long id);

    @EntityGraph(attributePaths = {"user", "media"})
    @Query("""
            select s from Story s
            where s.active = true
              and s.expiresAt > :now
              and (s.user.id = :viewerId
                   or s.user.id in (
                        select f.following.id from Follow f
                        where f.follower.id = :viewerId
                          and f.status = com.linkup.follow.FollowStatus.ACCEPTED
                   ))
            order by s.createdAt desc
    """)
    List<Story> findVisibleStories(Long viewerId, Instant now);

    @Modifying
    @Query("update Story s set s.active = false where s.active = true and s.expiresAt <= :now")
    int deactivateExpired(Instant now);
}
