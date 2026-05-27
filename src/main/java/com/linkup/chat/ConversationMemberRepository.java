package com.linkup.chat;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {
    boolean existsByIdConversationIdAndIdUserId(Long conversationId, Long userId);
    Optional<ConversationMember> findByIdConversationIdAndIdUserId(Long conversationId, Long userId);
}
