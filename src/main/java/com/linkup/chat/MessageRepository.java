package com.linkup.chat;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @EntityGraph(attributePaths = "sender")
    List<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    List<Message> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(Long conversationId, Instant before, Pageable pageable);

    @Query(value = """
            select count(*)
            from messages m
            where m.sender_id in (:viewerId, :authorId)
              and exists (
                    select 1 from conversation_members cm_viewer
                    where cm_viewer.conversation_id = m.conversation_id
                      and cm_viewer.user_id = :viewerId
              )
              and exists (
                    select 1 from conversation_members cm_author
                    where cm_author.conversation_id = m.conversation_id
                      and cm_author.user_id = :authorId
              )
            """, nativeQuery = true)
    long countMessagesBetween(@Param("viewerId") Long viewerId, @Param("authorId") Long authorId);
}
