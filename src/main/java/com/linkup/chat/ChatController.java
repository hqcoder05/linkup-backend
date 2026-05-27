package com.linkup.chat;

import com.linkup.chat.dto.ChatDtos.ConversationDto;
import com.linkup.chat.dto.ChatDtos.CreateConversationRequest;
import com.linkup.chat.dto.ChatDtos.MessageDto;
import com.linkup.chat.dto.ChatDtos.SendMessageRequest;
import com.linkup.common.ApiResponse;
import com.linkup.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ChatController {
    private final ChatService chatService;
    private final CurrentUser currentUser;

    public ChatController(ChatService chatService, CurrentUser currentUser) {
        this.chatService = chatService;
        this.currentUser = currentUser;
    }

    @PostMapping
    ApiResponse<ConversationDto> create(@Valid @RequestBody CreateConversationRequest request, Authentication authentication) {
        return ApiResponse.ok(chatService.createConversation(currentUser.id(authentication), request));
    }

    @GetMapping
    ApiResponse<List<ConversationDto>> mine(Authentication authentication) {
        return ApiResponse.ok(chatService.conversations(currentUser.id(authentication)));
    }

    @GetMapping("/{conversationId}/messages")
    ApiResponse<List<MessageDto>> messages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(defaultValue = "50") int take,
            Authentication authentication) {
        return ApiResponse.ok(chatService.messages(conversationId, currentUser.id(authentication), before, take));
    }

    @PostMapping("/{conversationId}/messages")
    ApiResponse<MessageDto> send(@PathVariable Long conversationId, @Valid @RequestBody SendMessageRequest request, Authentication authentication) {
        return ApiResponse.ok(chatService.sendMessage(conversationId, currentUser.id(authentication), request));
    }

    @PostMapping("/{conversationId}/read")
    ApiResponse<Void> read(@PathVariable Long conversationId, Authentication authentication) {
        chatService.markRead(conversationId, currentUser.id(authentication));
        return ApiResponse.message("Marked as read");
    }
}
