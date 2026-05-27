package com.linkup.chat;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @EntityGraph(attributePaths = {"members", "members.user"})
    @Query("select distinct c from Conversation c join c.members m where m.user.id = :userId order by coalesce(c.updatedAt, c.createdAt) desc")
    List<Conversation> findUserConversations(Long userId);
}
