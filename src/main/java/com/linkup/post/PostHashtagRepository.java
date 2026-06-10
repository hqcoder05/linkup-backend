package com.linkup.post;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {
    @EntityGraph(attributePaths = "hashtag")
    List<PostHashtag> findByPostId(Long postId);

    @EntityGraph(attributePaths = {"post", "post.user", "post.media"})
    List<PostHashtag> findByHashtagNameOrderByPostCreatedAtDesc(String name, Pageable pageable);

    @Query(value = """
            select distinct h.name
            from hashtags h
            join post_hashtags ph on ph.hashtag_id = h.id
            join posts p on p.id = ph.post_id
            left join likes l on l.post_id = p.id and l.user_id = :userId
            where p.user_id = :userId
               or l.id is not null
            """, nativeQuery = true)
    List<String> findInterestHashtagsForUser(@Param("userId") Long userId);

    @Query(value = """
            select p.user_id as userId, count(distinct h.name)::int as sharedCount
            from posts p
            join post_hashtags ph on ph.post_id = p.id
            join hashtags h on h.id = ph.hashtag_id
            where p.user_id in (:candidateIds)
              and h.name in (:hashtags)
            group by p.user_id
            """, nativeQuery = true)
    List<UserHashtagOverlapProjection> countSharedHashtagsByCandidates(
            @Param("candidateIds") List<Long> candidateIds,
            @Param("hashtags") List<String> hashtags);

    interface UserHashtagOverlapProjection {
        Long getUserId();
        int getSharedCount();
    }
}
