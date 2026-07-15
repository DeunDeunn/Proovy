package com.deundeun.chat.controller;

import com.deundeun.chat.dto.request.ChatMessageSendRequest;
import com.deundeun.chat.dto.response.ChatMessageCreatedEvent;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import com.deundeun.chat.service.ChatMessageService;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private static final String BROADCAST_DESTINATION_PREFIX = "/topic/chats/rooms/";

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chats/rooms/{chatRoomId}/messages")
    public void sendMessage(
        @DestinationVariable Long chatRoomId,
        @Payload ChatMessageSendRequest request,
        Principal principal
    ) {
        Long senderId = resolveSenderId(principal);
        ChatMessageResponse response = chatMessageService.send(chatRoomId, senderId, request);

        messagingTemplate.convertAndSend(BROADCAST_DESTINATION_PREFIX + chatRoomId, ChatMessageCreatedEvent.of(response));
    }

    private Long resolveSenderId(Principal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        return Long.valueOf(principal.getName());
    }
}
