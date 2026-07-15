package com.deundeun.global.websocket;

import com.deundeun.chat.service.ChatRoomMemberService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class ChatSubscribeChannelInterceptor implements ChannelInterceptor {

    private static final String CHAT_ROOM_DESTINATION_PATTERN = "/topic/chats/rooms/{chatRoomId}";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ChatRoomMemberService chatRoomMemberService;

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateChatRoomAccess(accessor);
        }

        return message;
    }

    private void validateChatRoomAccess(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !PATH_MATCHER.match(CHAT_ROOM_DESTINATION_PATTERN, destination)) {
            return;
        }

        Long chatRoomId = Long.valueOf(
            PATH_MATCHER.extractUriTemplateVariables(CHAT_ROOM_DESTINATION_PATTERN, destination)
                .get("chatRoomId")
        );

        Principal principal = accessor.getUser();
        Long userId = Long.valueOf(principal.getName());

        chatRoomMemberService.getChatRoomMember(chatRoomId, userId);
    }
}
