package com.deundeun.chat.service.support;

import com.deundeun.chat.dto.response.ChatMessageCreatedEvent;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageBroadcaster {

    private static final String BROADCAST_DESTINATION_PREFIX = "/topic/chats/rooms/";

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(Long chatRoomId, ChatMessageResponse response) {
        try {
            messagingTemplate.convertAndSend(BROADCAST_DESTINATION_PREFIX + chatRoomId, ChatMessageCreatedEvent.of(response));
        } catch (Exception e) {
            log.error("[Chat] 메시지 저장은 성공했으나 브로드캐스트에 실패했습니다: chatRoomId={}, messageId={}",
                chatRoomId, response.messageId(), e);
        }
    }
}
