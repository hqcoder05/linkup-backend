package com.linkup.chat;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @EntityGraph(attributePaths = "sender")
    List<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    List<Message> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(Long conversationId, Instant before, Pageable pageable);
}
