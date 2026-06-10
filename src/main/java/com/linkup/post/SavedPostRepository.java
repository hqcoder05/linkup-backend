package com.linkup.post;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPostRepository extends JpaRepository<SavedPost, SavedPostId> {
    @EntityGraph(attributePaths = {"post", "post.user", "post.media"})
    List<SavedPost> findByIdUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
