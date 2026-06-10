package com.linkup.post;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedPostRepository extends JpaRepository<SavedPost, SavedPostId> {
    @EntityGraph(attributePaths = {"post", "post.user", "post.media"})
    List<SavedPost> findByIdUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            insert into saved_posts(user_id, post_id, created_at)
            values (:userId, :postId, now())
            on conflict (user_id, post_id) do nothing
            """, nativeQuery = true)
    int savePost(@Param("userId") Long userId, @Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from saved_posts where user_id = :userId and post_id = :postId", nativeQuery = true)
    int unsavePost(@Param("userId") Long userId, @Param("postId") Long postId);
}
