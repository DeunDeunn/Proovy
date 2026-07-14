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
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomMemberService {

    private final ChatRoomMemberMapper chatRoomMemberMapper;

    @Transactional
    public void join(Long chatRoomId, Long userId) {
        try {
            rejoin(chatRoomId, userId);
        } catch (NoSuchElementException e) {
            chatRoomMemberMapper.insert(ChatRoomMember.join(chatRoomId, userId));
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

    private void rejoin(Long chatRoomId, Long userId) {
        ChatRoomMember member = chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, userId)
            .orElseThrow(NoSuchElementException::new);

        validateNotJoined(member);
        chatRoomMemberMapper.rejoin(chatRoomId, userId);
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
