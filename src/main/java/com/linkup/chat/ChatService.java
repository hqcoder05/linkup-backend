package com.linkup.chat;

import com.linkup.chat.dto.ChatDtos.ConversationDto;
import com.linkup.chat.dto.ChatDtos.CreateConversationRequest;
import com.linkup.chat.dto.ChatDtos.MessageDto;
import com.linkup.chat.dto.ChatDtos.SendMessageRequest;
import com.linkup.common.BadRequestException;
import com.linkup.common.ForbiddenException;
import com.linkup.common.ResourceNotFoundException;
import com.linkup.notification.NotificationService;
import com.linkup.user.User;
import com.linkup.user.UserMapper;
import com.linkup.user.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public ChatService(ConversationRepository conversationRepository, ConversationMemberRepository memberRepository, MessageRepository messageRepository, UserService userService, SimpMessagingTemplate messagingTemplate, NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    @Transactional
    public ConversationDto createConversation(Long currentUserId, CreateConversationRequest request) {
        LinkedHashSet<Long> memberIds = new LinkedHashSet<>(request.memberIds());
        memberIds.add(currentUserId);
        if (memberIds.size() < 2) {
            throw new BadRequestException("At least two members are required");
        }
        Conversation conversation = new Conversation();
        conversation.setName(request.name());
        conversation.setGroupConversation(memberIds.size() > 2);
        conversation = conversationRepository.save(conversation);

        List<ConversationMember> members = new ArrayList<>();
        for (Long memberId : memberIds) {
            ConversationMember member = new ConversationMember();
            member.setId(new ConversationMemberId(conversation.getId(), memberId));
            member.setConversation(conversation);
            member.setUser(userService.get(memberId));
            members.add(memberRepository.save(member));
        }
        conversation.setMembers(members);
        return toConversationDto(conversation, currentUserId);
    }

    public List<ConversationDto> conversations(Long userId) {
        return conversationRepository.findUserConversations(userId).stream().map(c -> toConversationDto(c, userId)).toList();
    }

    public List<MessageDto> messages(Long conversationId, Long userId, Instant before, int take) {
        ensureMember(conversationId, userId);
        List<Message> messages = before == null
                ? messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, take))
                : messageRepository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(conversationId, before, PageRequest.of(0, take));
        Instant lastReadAt = memberRepository.findByIdConversationIdAndIdUserId(conversationId, userId).map(ConversationMember::getLastReadAt).orElse(null);
        return messages.stream().map(m -> toMessageDto(m, userId, lastReadAt)).toList();
    }

    @Transactional
    public MessageDto sendMessage(Long conversationId, Long userId, SendMessageRequest request) {
        ensureMember(conversationId, userId);
        if ((request.content() == null || request.content().isBlank()) && (request.attachmentUrl() == null || request.attachmentUrl().isBlank())) {
            throw new BadRequestException("Message must have content or attachment");
        }
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        User sender = userService.get(userId);
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.content());
        message.setAttachmentUrl(request.attachmentUrl());
        Message saved = messageRepository.save(message);
        MessageDto dto = toMessageDto(saved, userId, null);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, dto);
        conversation.getMembers().stream()
                .map(m -> m.getUser().getId())
                .filter(id -> !id.equals(userId))
                .forEach(id -> notificationService.create(id, "message", "New message", sender.getFullName() + " sent you a message.", "/conversations/" + conversationId));
        return dto;
    }

    @Transactional
    public void markRead(Long conversationId, Long userId) {
        ConversationMember member = memberRepository.findByIdConversationIdAndIdUserId(conversationId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this conversation"));
        member.setLastReadAt(Instant.now());
    }

    public void ensureMember(Long conversationId, Long userId) {
        if (!memberRepository.existsByIdConversationIdAndIdUserId(conversationId, userId)) {
            throw new ForbiddenException("Not a member of this conversation");
        }
    }

    private ConversationDto toConversationDto(Conversation conversation, Long userId) {
        MessageDto lastMessage = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), PageRequest.of(0, 1))
                .stream().findFirst().map(m -> toMessageDto(m, userId, null)).orElse(null);
        return new ConversationDto(
                conversation.getId(),
                conversation.getName(),
                conversation.isGroupConversation(),
                conversation.getMembers().stream().map(ConversationMember::getUser).map(UserMapper::toDto).toList(),
                lastMessage,
                0,
                conversation.getCreatedAt());
    }

    private MessageDto toMessageDto(Message message, Long currentUserId, Instant lastReadAt) {
        boolean read = lastReadAt != null && message.getCreatedAt() != null && !message.getCreatedAt().isAfter(lastReadAt);
        return new MessageDto(
                message.getId(),
                message.getConversation().getId(),
                UserMapper.toDto(message.getSender()),
                message.getContent(),
                message.getAttachmentUrl(),
                message.getCreatedAt(),
                message.isDeleted(),
                read,
                read ? lastReadAt : null,
                message.getSender().getId().equals(currentUserId));
    }
}
