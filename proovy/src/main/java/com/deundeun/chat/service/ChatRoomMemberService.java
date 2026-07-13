package com.deundeun.chat.service;

import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomMemberService {

    private final ChatRoomMemberMapper chatRoomMemberMapper;

    @Transactional
    public void join(Long chatRoomId, Long userId) {
        Optional<ChatRoomMember> existing = chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, userId);

        if (existing.isEmpty()) { //없으면 join 처리
            ChatRoomMember member = ChatRoomMember.join(chatRoomId, userId);
            chatRoomMemberMapper.insert(member);
        } else { //있으면 재입장 가능한 상태인지 검증 후 rejoin 처리
            validateNotJoined(existing.get());
            chatRoomMemberMapper.rejoin(chatRoomId, userId);
        }

        log.info("[Chat] 채팅방 참여 완료: chatRoomId={}, userId={}", chatRoomId, userId);
    }

    @Transactional
    public void leave(Long chatRoomId, Long userId) {
        ChatRoomMember member = getChatRoomMember(chatRoomId, userId);

        validateActive(member);

        chatRoomMemberMapper.leave(chatRoomId, userId, LocalDateTime.now());
        log.info("[Chat] 채팅방 탈퇴 완료: chatRoomId={}, userId={}", chatRoomId, userId);
    }

    public ChatRoomMember getChatRoomMember(Long chatRoomId, Long userId) {
        return chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, userId)
            .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN));
    }

    private void validateActive(ChatRoomMember member) {
        if (!member.isActive()) {
            throw new ApiException(ErrorCode.CHAT_ROOM_ALREADY_LEFT);
        }
    }

    private void validateNotJoined(ChatRoomMember member) {
        if (member.isActive()) {
            throw new ApiException(ErrorCode.CHAT_ROOM_ALREADY_JOINED);
        }
    }
}
