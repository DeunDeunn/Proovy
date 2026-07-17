package com.deundeun.chat.service.support;

import com.deundeun.chat.constant.ChatStompDestinations;
import com.deundeun.chat.dto.response.ChatMessageCreatedEvent;
import com.deundeun.chat.dto.response.ChatMessageDeleteResponse;
import com.deundeun.chat.dto.response.ChatMessageDeletedEvent;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(Long chatRoomId, ChatMessageResponse response) {
        try {
            messagingTemplate.convertAndSend(ChatStompDestinations.roomTopic(chatRoomId), ChatMessageCreatedEvent.of(response));
        } catch (Exception e) {
            log.error("[Chat] 메시지 저장은 성공했으나 브로드캐스트에 실패했습니다: chatRoomId={}, messageId={}",
                chatRoomId, response.messageId(), e);
        }
    }

    public void broadcast(ChatMessageDeleteResponse response) {
        try {
            messagingTemplate.convertAndSend(ChatStompDestinations.roomTopic(response.chatRoomId()), ChatMessageDeletedEvent.of(response));
        } catch (Exception e) {
            log.error("[Chat] 메시지 삭제는 성공했으나 브로드캐스트에 실패했습니다: chatRoomId={}, messageId={}",
                response.chatRoomId(), response.messageId(), e);
        }
    }
}
