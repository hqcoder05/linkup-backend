package com.linkup.post;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = {"user", "media"})
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "media"})
    List<Post> findByCaptionContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);

    long countByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "media"})
    List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "media"})
    @Query("""
            select p from Post p
            where p.user.id = :userId
               or p.user.id in (
                    select f.following.id from Follow f
                    where f.follower.id = :userId
                      and f.status = com.linkup.follow.FollowStatus.ACCEPTED
               )
            order by p.createdAt desc
    """)
    List<Post> findFeed(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "media"})
    @Query(value = """
            select p.*
            from posts p
            join users u on u.id = p.user_id
            where u.private_account = false
               or p.user_id = :viewerId
               or exists (
                    select 1
                    from follows f
                    where f.follower_id = :viewerId
                      and f.following_id = p.user_id
                      and f.status = 'ACCEPTED'
               )
            order by (
                (p.likes_count + p.comments_count * 2.0)
                / power((extract(epoch from (now() - p.created_at)) / 3600.0 + 2.0), 1.5)
            ) desc,
            p.created_at desc
            """, nativeQuery = true)
    List<Post> findExplore(Long viewerId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likesCount = p.likesCount + 1 where p.id = :id")
    int incrementLikesCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likesCount = case when p.likesCount > 0 then p.likesCount - 1 else 0 end where p.id = :id")
    int decrementLikesCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.commentsCount = p.commentsCount + 1 where p.id = :id")
    int incrementCommentsCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.commentsCount = case when p.commentsCount > 0 then p.commentsCount - 1 else 0 end where p.id = :id")
    int decrementCommentsCount(@Param("id") Long id);

    @Query("select p.likesCount from Post p where p.id = :id")
    int findLikesCountById(@Param("id") Long id);

    @Query("select p.commentsCount from Post p where p.id = :id")
    int findCommentsCountById(@Param("id") Long id);
}
