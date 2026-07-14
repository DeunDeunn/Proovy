package com.deundeun.chat.service.support;

import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomMemberFinder {

    private final ChatRoomMemberMapper chatRoomMemberMapper;

    public ChatRoomMember findMember(Long chatRoomId, Long userId) {
        ChatRoomMember member = getMember(chatRoomId, userId);
        validateActive(member);

        return member;
    }

    private ChatRoomMember getMember(Long chatRoomId, Long userId) {
        return chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, userId)
            .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN));
    }

    private void validateActive(ChatRoomMember member) {
        if (!member.isActive()) {
            throw new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }
    }
}
