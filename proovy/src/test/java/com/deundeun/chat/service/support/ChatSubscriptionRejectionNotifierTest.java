package com.deundeun.chat.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.deundeun.chat.constant.ChatStompDestinations;
import com.deundeun.chat.dto.response.ChatSubscribeFailedEvent;
import com.deundeun.chat.event.ChatSubscriptionRejectedEvent;
import com.deundeun.global.exception.ErrorCode;

@DisplayName("ChatSubscriptionRejectionNotifier")
@ExtendWith(MockitoExtension.class)
class ChatSubscriptionRejectionNotifierTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatSubscriptionRejectionNotifier notifier;

    @Test
    @DisplayName("구독 거부 이벤트를 받으면 개인 에러 큐로 실패 이벤트를 전송한다")
    void handle_sendsFailedEventToPersonalErrorQueue() {
        ChatSubscriptionRejectedEvent event = new ChatSubscriptionRejectedEvent("10", ErrorCode.CHAT_ROOM_FORBIDDEN);

        notifier.handle(event);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(
            eq("10"), eq(ChatStompDestinations.PERSONAL_ERROR_QUEUE), payloadCaptor.capture());

        ChatSubscribeFailedEvent payload = (ChatSubscribeFailedEvent) payloadCaptor.getValue();
        assertThat(payload.eventType()).isEqualTo("CHAT_SUBSCRIBE_FAILED");
        assertThat(payload.status()).isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN.getStatus().value());
        assertThat(payload.code()).isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN.getCode());
        assertThat(payload.message()).isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN.getMessage());
    }
}
