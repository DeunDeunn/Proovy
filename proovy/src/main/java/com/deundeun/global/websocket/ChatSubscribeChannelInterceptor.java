package com.deundeun.global.websocket;

import com.deundeun.chat.constant.ChatStompDestinations;
import com.deundeun.chat.dto.response.ChatSubscribeFailedEvent;
import com.deundeun.chat.service.ChatRoomMemberService;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSubscribeChannelInterceptor implements ChannelInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ChatRoomMemberService chatRoomMemberService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        /**
         * accessor != null : StompHeaderAccessor 형식으로 해석 가능함.
         * accessor == null : 해석 불가능. 메시지가 STOMP 프레임 구조가 아님. 시스템 정상 작동을 위한 내부 메시지일 가능성.
         * */
        if (accessor == null) {
            return message;
        }

        String destination = accessor.getDestination();

        /**
         * destination == null : CONNECT/DISCONNECT/ACK 등 destination 자체가 없는 프레임.
         * 이 인터셉터가 검증할 대상이 아님.
         * destination이 "/topic"으로 시작하지 않음 : 이 인터셉터가 검증하는 구독 경로(/topic) 밖의 destination.
         * 두 경우 모두 검증 없이 그대로 통과시킨다.
         * */
        if (destination == null || !destination.startsWith("/topic")) {
            return message;
        }

        try {
            validateTopicAccess(accessor, destination);
            return message;
        } catch (ApiException e) {
            log.warn("STOMP 구독 거부: {} - {}", e.getErrorCode().getCode(), e.getMessage(), e);
            notifySubscribeFailed(accessor.getUser(), e.getErrorCode());
            return null;
        }
    }

    private void notifySubscribeFailed(Principal principal, ErrorCode errorCode) {
        if (principal == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(
            principal.getName(), ChatStompDestinations.PERSONAL_ERROR_QUEUE, ChatSubscribeFailedEvent.of(errorCode));
    }

    private void validateTopicAccess(StompHeaderAccessor accessor, String destination) {
        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        validateChatRoomAccess(accessor, destination);
    }

    private void validateChatRoomAccess(StompHeaderAccessor accessor, String destination) {
        if (!PATH_MATCHER.match(ChatStompDestinations.ROOM_TOPIC_PATTERN, destination)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        String chatRoomIdValue = PATH_MATCHER
            .extractUriTemplateVariables(ChatStompDestinations.ROOM_TOPIC_PATTERN, destination)
            .get("chatRoomId");
        Long chatRoomId = parseChatRoomId(chatRoomIdValue);
        Long userId = resolveUserId(accessor);

        chatRoomMemberService.getChatRoomMember(chatRoomId, userId);
    }

    private Long parseChatRoomId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private Long resolveUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return Long.valueOf(principal.getName());
    }
}
