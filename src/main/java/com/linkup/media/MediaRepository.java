package com.linkup.media;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByPostIdOrderByPositionAsc(Long postId);
}
