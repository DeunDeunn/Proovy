package com.deundeun.chat.controller;

import com.deundeun.chat.dto.request.ChatMessageSendRequest;
import com.deundeun.chat.dto.response.ChatMessageCreatedEvent;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import com.deundeun.chat.service.ChatMessageService;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private static final String BROADCAST_DESTINATION_PREFIX = "/topic/chats/rooms/";

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chats/rooms/{chatRoomId}/messages")
    public void sendMessage(
        @DestinationVariable Long chatRoomId,
        @Payload @Valid ChatMessageSendRequest request,
        Principal principal
    ) {
        Long senderId = resolveSenderId(principal);
        ChatMessageResponse response = chatMessageService.send(chatRoomId, senderId, request);

        broadcast(chatRoomId, response);
    }

    private void broadcast(Long chatRoomId, ChatMessageResponse response) {
        try {
            messagingTemplate.convertAndSend(BROADCAST_DESTINATION_PREFIX + chatRoomId, ChatMessageCreatedEvent.of(response));
        } catch (Exception e) {
            log.error("[Chat] 메시지 저장은 성공했으나 브로드캐스트에 실패했습니다: chatRoomId={}, messageId={}",
                chatRoomId, response.messageId(), e);
        }
    }

    private Long resolveSenderId(Principal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        return Long.valueOf(principal.getName());
    }
}
