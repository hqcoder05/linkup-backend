package com.linkup.websocket;

import com.linkup.chat.ChatService;
import com.linkup.common.BadRequestException;
import com.linkup.user.UserMapper;
import com.linkup.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class CallSignalController {
    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public CallSignalController(ChatService chatService, UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/calls.invite")
    public void invite(@Valid @Payload CallInviteRequest request) {
        validateParticipants(request.conversationId(), request.callerId(), request.receiverId());
        String callId = request.callId() == null || request.callId().isBlank()
                ? UUID.randomUUID().toString()
                : request.callId();
        push(request.receiverId(), new CallSignalDto(
                "invite",
                callId,
                request.conversationId(),
                request.callerId(),
                request.receiverId(),
                request.callType(),
                null,
                null,
                UserMapper.toDto(userService.get(request.callerId())),
                Instant.now()));
    }

    @MessageMapping("/calls.offer")
    public void offer(@Valid @Payload SessionDescriptionRequest request) {
        relayDescription("offer", request);
    }

    @MessageMapping("/calls.answer")
    public void answer(@Valid @Payload SessionDescriptionRequest request) {
        relayDescription("answer", request);
    }

    @MessageMapping("/calls.ice")
    public void ice(@Valid @Payload IceCandidateRequest request) {
        validateParticipants(request.conversationId(), request.senderId(), request.receiverId());
        push(request.receiverId(), new CallSignalDto(
                "ice",
                request.callId(),
                request.conversationId(),
                request.senderId(),
                request.receiverId(),
                null,
                null,
                request.candidate(),
                UserMapper.toDto(userService.get(request.senderId())),
                Instant.now()));
    }

    @MessageMapping("/calls.reject")
    public void reject(@Valid @Payload CallStateRequest request) {
        relayState("reject", request);
    }

    @MessageMapping("/calls.end")
    public void end(@Valid @Payload CallStateRequest request) {
        relayState("end", request);
    }

    private void relayDescription(String event, SessionDescriptionRequest request) {
        validateParticipants(request.conversationId(), request.senderId(), request.receiverId());
        push(request.receiverId(), new CallSignalDto(
                event,
                request.callId(),
                request.conversationId(),
                request.senderId(),
                request.receiverId(),
                null,
                request.sdp(),
                null,
                UserMapper.toDto(userService.get(request.senderId())),
                Instant.now()));
    }

    private void relayState(String event, CallStateRequest request) {
        validateParticipants(request.conversationId(), request.senderId(), request.receiverId());
        push(request.receiverId(), new CallSignalDto(
                event,
                request.callId(),
                request.conversationId(),
                request.senderId(),
                request.receiverId(),
                null,
                null,
                null,
                UserMapper.toDto(userService.get(request.senderId())),
                Instant.now()));
    }

    private void validateParticipants(Long conversationId, Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new BadRequestException("Call receiver must be different from sender");
        }
        chatService.ensureMember(conversationId, senderId);
        chatService.ensureMember(conversationId, receiverId);
    }

    private void push(Long receiverId, CallSignalDto payload) {
        messagingTemplate.convertAndSend("/topic/calls/" + receiverId, payload);
    }

    public record CallInviteRequest(
            String callId,
            @NotNull Long conversationId,
            @NotNull Long callerId,
            @NotNull Long receiverId,
            @NotBlank String callType
    ) {
    }

    public record SessionDescriptionRequest(
            @NotBlank String callId,
            @NotNull Long conversationId,
            @NotNull Long senderId,
            @NotNull Long receiverId,
            @NotBlank String sdp
    ) {
    }

    public record IceCandidateRequest(
            @NotBlank String callId,
            @NotNull Long conversationId,
            @NotNull Long senderId,
            @NotNull Long receiverId,
            @NotBlank String candidate
    ) {
    }

    public record CallStateRequest(
            @NotBlank String callId,
            @NotNull Long conversationId,
            @NotNull Long senderId,
            @NotNull Long receiverId
    ) {
    }

    public record CallSignalDto(
            String event,
            String callId,
            Long conversationId,
            Long senderId,
            Long receiverId,
            String callType,
            String sdp,
            String candidate,
            com.linkup.user.dto.UserDto sender,
            Instant createdAt
    ) {
    }
}
