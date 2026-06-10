package com.linkup.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query(value = """
            select u.*
            from users u
            left join follows f1
                on f1.following_id = u.id
               and f1.follower_id = :currentUserId
               and f1.status = 'ACCEPTED'
            left join follows f2
                on f2.follower_id = u.id
               and f2.following_id = :currentUserId
               and f2.status = 'ACCEPTED'
            where u.id <> :currentUserId
              and u.active = true
              and u.search_indexing_enabled = true
              and (
                    u.full_name ilike :keywordWildcard escape '\\'
                 or u.email ilike :keywordWildcard escape '\\'
                 or similarity(u.full_name, :keywordRaw) > 0.25
              )
            order by (
                case when f1.follower_id is not null then 10 else 0 end
              + case when f2.following_id is not null then 5 else 0 end
              + case when u.full_name ilike :keywordPrefix escape '\\' then 3 else 0 end
              + case when exists (
                    select 1
                    from follows viewer_following
                    join follows candidate_followers
                      on candidate_followers.follower_id = viewer_following.following_id
                     and candidate_followers.following_id = u.id
                     and candidate_followers.status = 'ACCEPTED'
                    where viewer_following.follower_id = :currentUserId
                      and viewer_following.status = 'ACCEPTED'
                ) then 2 else 0 end
            ) desc,
            u.full_name asc,
            u.id asc
            limit 20
            """, nativeQuery = true)
    List<User> searchRanked(
            @Param("currentUserId") Long currentUserId,
            @Param("keywordPrefix") String keywordPrefix,
            @Param("keywordWildcard") String keywordWildcard,
            @Param("keywordRaw") String keywordRaw);

    @Query(value = """
            select u.*
            from users u
            where u.id in (:ids)
              and u.active = true
            """, nativeQuery = true)
    List<User> findAllByIdsUnordered(@Param("ids") List<Long> ids);

    @Query(value = """
            select u.*
            from users u
            left join follows f
              on f.following_id = u.id
             and f.status = 'ACCEPTED'
            where u.id <> :currentUserId
              and u.active = true
              and u.search_indexing_enabled = true
              and u.id not in (
                    select following_id
                    from follows
                    where follower_id = :currentUserId
                      and status in ('ACCEPTED', 'PENDING')
              )
            group by u.id
            order by count(f.follower_id) desc, u.created_at desc
            limit :limit
            """, nativeQuery = true)
    List<User> findTrendingSuggestionFallback(@Param("currentUserId") Long currentUserId, @Param("limit") int limit);
}
