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
    @DisplayName("채팅방 destination이 아니면 검증 없이 통과한다")
    void preSend_subscribeToNonChatRoomDestination_passesThroughWithoutValidation() {
        Message<byte[]> message = subscribeMessage("/user/queue/errors", 1L);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
    }

    private Message<byte[]> subscribeMessage(String destination, Long userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(() -> String.valueOf(userId));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
