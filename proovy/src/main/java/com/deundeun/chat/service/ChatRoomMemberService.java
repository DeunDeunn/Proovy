package com.deundeun.chat.service;

import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.chat.service.support.ChatRoomMemberFinder;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomMemberService {

    private final ChatRoomMemberMapper chatRoomMemberMapper;
    private final ChatRoomMemberFinder chatRoomMemberFinder;

    @Transactional
    public void markAsRead(Long chatRoomId, Long userId, Long lastReadMessageId) {
        ChatRoomMember member = chatRoomMemberFinder.findMember(chatRoomId, userId);

        member.markRead(lastReadMessageId);
        chatRoomMemberMapper.updateLastRead(member);
    }

    @Transactional
    public void join(Long chatRoomId, Long userId) {
        chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, userId)
            .ifPresentOrElse(
                this::rejoinIfInactive,
                () -> insertNewMember(chatRoomId, userId)
            );
    }

    @Transactional
    public void leave(Long chatRoomId, Long userId) {
        ChatRoomMember member = getChatRoomMember(chatRoomId, userId);

        if (!member.isActive()) {
            return; // 이미 나간 상태 — 멱등 처리
        }

        member.leave();
        chatRoomMemberMapper.leave(member);
        log.info("[Chat] 채팅방 탈퇴 완료: chatRoomId={}, userId={}", chatRoomId, userId);
    }

    public ChatRoomMember getChatRoomMember(Long chatRoomId, Long userId) {
        return chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, userId)
            .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN));
    }

    private void insertNewMember(Long chatRoomId, Long userId) {
        ChatRoomMember newMember = ChatRoomMember.join(chatRoomId, userId);

        try {
            chatRoomMemberMapper.insert(newMember);
            log.info("[Chat] 채팅방 신규 참여 완료: chatRoomId={}, userId={}", chatRoomId, userId);
        } catch (DuplicateKeyException e) {
            log.debug("[Chat] 동시 요청으로 인한 멤버 중복 생성 시도, 무시: chatRoomId={}, userId={}", chatRoomId, userId);
        }
    }

    private void rejoinIfInactive(ChatRoomMember member) {
        if (member.isActive()) {
            log.debug("[Chat] 이미 활성 멤버, 재참여 처리 스킵: chatRoomId={}, userId={}", member.getChatRoomId(), member.getUserId());
            return;
        }

        member.rejoin();
        chatRoomMemberMapper.rejoin(member);
        log.info("[Chat] 채팅방 재참여 완료: chatRoomId={}, userId={}", member.getChatRoomId(), member.getUserId());
    }
}
