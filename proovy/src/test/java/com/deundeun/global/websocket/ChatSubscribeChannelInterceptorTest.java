package com.deundeun.global.websocket;

import com.deundeun.chat.service.ChatRoomMemberService;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ChatSubscribeChannelInterceptorTest {

    @Mock
    private ChatRoomMemberService chatRoomMemberService;

    @InjectMocks
    private ChatSubscribeChannelInterceptor interceptor;

    @Test
    @DisplayName("참여자가 아니면 SUBSCRIBE가 거부된다")
    void preSend_subscribeByNonMember_throwsException() {
        Message<byte[]> message = subscribeMessage("/topic/chats/rooms/10", 99L);
        doThrow(new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN))
                .when(chatRoomMemberService).getChatRoomMember(10L, 99L);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("참여자면 SUBSCRIBE가 그대로 통과한다")
    void preSend_subscribeByMember_passesThrough() {
        Message<byte[]> message = subscribeMessage("/topic/chats/rooms/10", 1L);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
    }

    @Test
    @DisplayName("/topic이 아닌 destination은 검증 없이 통과한다")
    void preSend_subscribeToNonTopicDestination_passesThroughWithoutValidation() {
        Message<byte[]> message = subscribeMessage("/user/queue/errors", 1L);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
    }

    @Test
    @DisplayName("화이트리스트에 없는 /topic destination은 거부된다")
    void preSend_subscribeToNonWhitelistedTopicDestination_throwsException() {
        Message<byte[]> message = subscribeMessage("/topic/other", 1L);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("클라이언트가 /topic으로 직접 SEND하면 거부된다")
    void preSend_directSendToTopic_throwsException() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/topic/chats/rooms/10", 1L);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("chatRoomId가 숫자 형식이 아니면 거부된다")
    void preSend_subscribeWithNonNumericChatRoomId_throwsException() {
        Message<byte[]> message = subscribeMessage("/topic/chats/rooms/not-a-number", 1L);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("Principal이 없으면 SUBSCRIBE가 거부된다")
    void preSend_subscribeWithoutPrincipal_throwsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chats/rooms/10");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(ApiException.class);
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
