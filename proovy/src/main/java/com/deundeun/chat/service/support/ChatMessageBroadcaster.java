package com.deundeun.chat.service.support;

import com.deundeun.chat.constant.ChatStompDestinations;
import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.dto.response.ChatMessageCreatedEvent;
import com.deundeun.chat.dto.response.ChatMessageDeleteResponse;
import com.deundeun.chat.dto.response.ChatMessageDeletedEvent;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import com.deundeun.chat.dto.response.ChatRoomReadEvent;
import com.deundeun.chat.dto.response.ChatRoomReadResponse;
import com.deundeun.chat.dto.response.ChatRoomUpdatedEvent;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomMemberMapper chatRoomMemberMapper;

    public void broadcast(Long chatRoomId, ChatMessageResponse response) {
        try {
            messagingTemplate.convertAndSend(ChatStompDestinations.roomTopic(chatRoomId), ChatMessageCreatedEvent.of(response));
        } catch (Exception e) {
            log.error("[Chat] 메시지 저장은 성공했으나 브로드캐스트에 실패했습니다: chatRoomId={}, messageId={}",
                chatRoomId, response.messageId(), e);
        }
        notifyRoomUpdated(chatRoomId, response.senderId());
    }

    // 방 토픽 구독은 지금 열어본 방 하나뿐이라, 다른 화면(사이드바 등)의 안 읽음 개수가
    // 갱신되려면 발신자를 제외한 참여자들에게 개인 큐로 별도 신호를 보내야 한다.
    private void notifyRoomUpdated(Long chatRoomId, Long senderId) {
        chatRoomMemberMapper.findActiveByChatRoomId(chatRoomId).stream()
            .map(ChatRoomMember::getUserId)
            .filter(userId -> !userId.equals(senderId))
            .forEach(userId -> {
                try {
                    messagingTemplate.convertAndSendToUser(
                        userId.toString(), ChatStompDestinations.PERSONAL_ROOM_UPDATE_QUEUE, ChatRoomUpdatedEvent.of(chatRoomId));
                } catch (Exception e) {
                    log.error("[Chat] 방 업데이트 개인 알림 전송에 실패했습니다: chatRoomId={}, userId={}", chatRoomId, userId, e);
                }
            });
    }

    public void broadcast(ChatMessageDeleteResponse response) {
        try {
            messagingTemplate.convertAndSend(ChatStompDestinations.roomTopic(response.chatRoomId()), ChatMessageDeletedEvent.of(response));
        } catch (Exception e) {
            log.error("[Chat] 메시지 삭제는 성공했으나 브로드캐스트에 실패했습니다: chatRoomId={}, messageId={}",
                response.chatRoomId(), response.messageId(), e);
        }
    }

    public void broadcastRead(ChatRoomReadResponse response) {
        try {
            messagingTemplate.convertAndSend(ChatStompDestinations.roomTopic(response.chatRoomId()), ChatRoomReadEvent.of(response));
        } catch (Exception e) {
            log.error("[Chat] 읽음 처리는 성공했으나 브로드캐스트에 실패했습니다: chatRoomId={}, userId={}",
                response.chatRoomId(), response.userId(), e);
        }
    }
}
