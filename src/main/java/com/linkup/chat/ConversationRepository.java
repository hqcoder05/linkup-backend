package com.linkup.chat;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @EntityGraph(attributePaths = {"members", "members.user"})
    @Query("select c from Conversation c join c.members m where m.user.id = :userId order by coalesce(c.updatedAt, c.createdAt) desc")
    List<Conversation> findUserConversations(Long userId);

    @EntityGraph(attributePaths = {"members", "members.user"})
    @Query("""
            select c
            from Conversation c
            join c.members m1
            join c.members m2
            where c.groupConversation = false
              and m1.user.id = :userId
              and m2.user.id = :otherUserId
              and size(c.members) = 2
            """)
    java.util.Optional<Conversation> findDirectConversation(Long userId, Long otherUserId);
}
