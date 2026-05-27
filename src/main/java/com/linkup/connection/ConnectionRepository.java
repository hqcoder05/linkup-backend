package com.linkup.connection;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConnectionRepository extends JpaRepository<Connection, ConnectionId> {
    @EntityGraph(attributePaths = {"requester", "addressee"})
    @Query("""
            select c from Connection c
            where ((c.requester.id = :userA and c.addressee.id = :userB)
                or (c.requester.id = :userB and c.addressee.id = :userA))
            """)
    Optional<Connection> findBetween(Long userA, Long userB);

    @EntityGraph(attributePaths = {"requester", "addressee"})
    @Query("""
            select c from Connection c
            where c.status = com.linkup.connection.ConnectionStatus.ACCEPTED
              and (c.requester.id = :userId or c.addressee.id = :userId)
            order by c.respondedAt desc nulls last, c.createdAt desc
            """)
    List<Connection> findAcceptedForUser(Long userId);

    @EntityGraph(attributePaths = {"requester", "addressee"})
    List<Connection> findByAddresseeIdAndStatusOrderByCreatedAtDesc(Long addresseeId, ConnectionStatus status);

    @EntityGraph(attributePaths = {"requester", "addressee"})
    List<Connection> findByRequesterIdAndStatusOrderByCreatedAtDesc(Long requesterId, ConnectionStatus status);
}
