package com.linkup.websocket;

import com.linkup.chat.ChatService;
import com.linkup.chat.dto.ChatDtos.MessageDto;
import com.linkup.chat.dto.ChatDtos.SendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class ChatSocketController {
    private final ChatService chatService;

    public ChatSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public MessageDto send(@Valid @Payload SocketMessageRequest request) {
        return chatService.sendMessage(request.conversationId(), request.senderId(), new SendMessageRequest(
                request.content(),
                request.attachmentUrl(),
                request.sharedPostId(),
                request.sharedStoryId(),
                request.disappearAfterSeconds()));
    }

    @MessageMapping("/conversations/{conversationId}/read")
    public void read(@DestinationVariable Long conversationId, @Payload SocketReadRequest request) {
        chatService.markRead(conversationId, request.userId());
    }

    public record SocketMessageRequest(Long conversationId, Long senderId, String content, String attachmentUrl, Long sharedPostId, Long sharedStoryId, Long disappearAfterSeconds) {
    }

    public record SocketReadRequest(Long userId) {
    }
}
