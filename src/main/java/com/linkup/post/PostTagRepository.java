package com.linkup.post;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {
    @EntityGraph(attributePaths = "taggedUser")
    List<PostTag> findByPostId(Long postId);
}
