package com.deundeun.chat.service.validator;

import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomMemberValidator {

    private final ChatRoomMemberMapper chatRoomMemberMapper;

    public void validateMember(Long chatRoomId, Long userId) {
        ChatRoomMember member = getChatRoomMember(chatRoomId, userId);

        validateActive(member);
    }

    private ChatRoomMember getChatRoomMember(Long chatRoomId, Long userId) {
        return chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, userId)
            .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN));
    }

    private void validateActive(ChatRoomMember member) {
        if (!member.isActive()) {
            throw new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }
    }
}
