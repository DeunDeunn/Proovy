package com.deundeun.global.websocket;

import com.deundeun.chat.event.ChatSubscriptionRejectedEvent;
import com.deundeun.chat.service.ChatRoomMemberService;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatSubscribeChannelInterceptorTest {

    @Mock
    private ChatRoomMemberService chatRoomMemberService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatSubscribeChannelInterceptor interceptor;

    @Test
    @DisplayName("참여자가 아니면 SUBSCRIBE를 drop하고 구독 거부 이벤트를 발행한다")
    void preSend_subscribeByNonMember_dropsAndPublishesRejectedEvent() {
        Message<byte[]> message = subscribeMessage("/topic/chats/rooms/10", 99L);
        doThrow(new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN))
                .when(chatRoomMemberService).getChatRoomMember(10L, 99L);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        ArgumentCaptor<ChatSubscriptionRejectedEvent> eventCaptor = ArgumentCaptor.forClass(ChatSubscriptionRejectedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().username()).isEqualTo("99");
        assertThat(eventCaptor.getValue().errorCode()).isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("참여자면 SUBSCRIBE가 그대로 통과한다")
    void preSend_subscribeByMember_passesThrough() {
        Message<byte[]> message = subscribeMessage("/topic/chats/rooms/10", 1L);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("/topic이 아닌 destination은 검증 없이 통과한다")
    void preSend_subscribeToNonTopicDestination_passesThroughWithoutValidation() {
        Message<byte[]> message = subscribeMessage("/user/queue/errors", 1L);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
    }

    @Test
    @DisplayName("화이트리스트에 없는 /topic destination은 drop되고 구독 거부 이벤트를 발행한다")
    void preSend_subscribeToNonWhitelistedTopicDestination_dropsAndPublishesRejectedEvent() {
        Message<byte[]> message = subscribeMessage("/topic/other", 1L);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        verify(eventPublisher).publishEvent(any(ChatSubscriptionRejectedEvent.class));
    }

    @Test
    @DisplayName("클라이언트가 /topic으로 직접 SEND하면 drop되고 구독 거부 이벤트를 발행한다")
    void preSend_directSendToTopic_dropsAndPublishesRejectedEvent() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/topic/chats/rooms/10", 1L);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        verify(eventPublisher).publishEvent(any(ChatSubscriptionRejectedEvent.class));
    }

    @Test
    @DisplayName("chatRoomId가 숫자 형식이 아니면 drop되고 구독 거부 이벤트를 발행한다")
    void preSend_subscribeWithNonNumericChatRoomId_dropsAndPublishesRejectedEvent() {
        Message<byte[]> message = subscribeMessage("/topic/chats/rooms/not-a-number", 1L);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        verify(eventPublisher).publishEvent(any(ChatSubscriptionRejectedEvent.class));
    }

    @Test
    @DisplayName("Principal이 없으면 SUBSCRIBE를 drop하고 알릴 대상이 없어 이벤트를 발행하지 않는다")
    void preSend_subscribeWithoutPrincipal_dropsWithoutPublishing() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chats/rooms/10");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNull();
        verify(eventPublisher, never()).publishEvent(any());
    }

    private Message<byte[]> subscribeMessage(String destination, Long userId) {
        return stompMessage(StompCommand.SUBSCRIBE, destination, userId);
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination, Long userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(() -> String.valueOf(userId));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
