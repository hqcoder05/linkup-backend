package com.linkup.like;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
    long countByUserIdAndPostUserId(Long userId, Long authorId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
}
