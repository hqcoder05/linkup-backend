package com.linkup.follow;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    @EntityGraph(attributePaths = {"follower", "following"})
    Optional<Follow> findByIdFollowerIdAndIdFollowingId(Long followerId, Long followingId);

    boolean existsByIdFollowerIdAndIdFollowingIdAndStatus(Long followerId, Long followingId, FollowStatus status);

    long countByIdFollowingIdAndStatus(Long followingId, FollowStatus status);

    long countByIdFollowerIdAndStatus(Long followerId, FollowStatus status);

    @EntityGraph(attributePaths = "follower")
    List<Follow> findByIdFollowingIdAndStatusOrderByCreatedAtDesc(Long followingId, FollowStatus status);

    @EntityGraph(attributePaths = "following")
    List<Follow> findByIdFollowerIdAndStatusOrderByCreatedAtDesc(Long followerId, FollowStatus status);

    @EntityGraph(attributePaths = {"follower", "following"})
    List<Follow> findByIdFollowingIdAndStatus(Long followingId, FollowStatus status);

    @Query("""
            select f.following.id from Follow f
            where f.follower.id = :userId and f.status = com.linkup.follow.FollowStatus.ACCEPTED
            """)
    List<Long> findAcceptedFollowingIds(Long userId);

    @Query(value = """
            select f2.following_id as userId, count(f2.following_id)::int as mutualCount
            from follows f1
            join follows f2 on f1.following_id = f2.follower_id
            where f1.follower_id = :currentUserId
              and f1.status = 'ACCEPTED'
              and f2.status = 'ACCEPTED'
              and f2.following_id <> :currentUserId
              and f2.following_id not in (
                    select following_id
                    from follows
                    where follower_id = :currentUserId
                      and status in ('ACCEPTED', 'PENDING')
              )
            group by f2.following_id
            order by mutualCount desc
            limit 50
            """, nativeQuery = true)
    List<SuggestionCandidateProjection> findMutualFollowCandidates(@Param("currentUserId") Long currentUserId);

    @Query(value = """
            select u.full_name as fullName
            from follows f1
            join follows f2
              on f1.following_id = f2.follower_id
             and f2.status = 'ACCEPTED'
            join users u on u.id = f1.following_id
            where f1.follower_id = :currentUserId
              and f1.status = 'ACCEPTED'
              and f2.following_id = :candidateId
            order by u.full_name asc
            limit 3
            """, nativeQuery = true)
    List<String> findMutualFriendNames(@Param("currentUserId") Long currentUserId, @Param("candidateId") Long candidateId);

    interface SuggestionCandidateProjection {
        Long getUserId();
        int getMutualCount();
    }
}
