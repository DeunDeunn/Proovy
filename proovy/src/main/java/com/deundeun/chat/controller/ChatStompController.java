package com.deundeun.chat.controller;

import com.deundeun.chat.dto.request.ChatMessageSendRequest;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import com.deundeun.chat.service.ChatMessageService;
import com.deundeun.chat.service.support.ChatMessageBroadcaster;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;
    private final ChatMessageBroadcaster chatMessageBroadcaster;

    @MessageMapping("/chats/rooms/{chatRoomId}/messages")
    public void sendMessage(
        @DestinationVariable Long chatRoomId,
        @Payload @Valid ChatMessageSendRequest request,
        Principal principal
    ) {
        Long senderId = resolveSenderId(principal);
        ChatMessageResponse response = chatMessageService.send(chatRoomId, senderId, request, null);

        chatMessageBroadcaster.broadcast(chatRoomId, response);
    }

    private Long resolveSenderId(Principal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        return Long.valueOf(principal.getName());
    }
}
