package com.linkup.post;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = "user")
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("""
            select p from Post p
            where p.user.id = :userId
               or p.user.id in (
                    select case
                        when c.requester.id = :userId then c.addressee.id
                        else c.requester.id
                    end
                    from Connection c
                    where c.status = com.linkup.connection.ConnectionStatus.ACCEPTED
                      and (c.requester.id = :userId or c.addressee.id = :userId)
               )
            order by p.createdAt desc
            """)
    List<Post> findFeed(Long userId, Pageable pageable);
}
