package com.linkup.story;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryViewRepository extends JpaRepository<StoryView, Long> {
    boolean existsByViewerIdAndStoryId(Long viewerId, Long storyId);

    @Query("select sv.story.id from StoryView sv where sv.viewer.id = :viewerId")
    List<Long> findSeenStoryIds(@Param("viewerId") Long viewerId);

    @EntityGraph(attributePaths = "viewer")
    List<StoryView> findByStoryIdOrderByCreatedAtDesc(Long storyId);
}
