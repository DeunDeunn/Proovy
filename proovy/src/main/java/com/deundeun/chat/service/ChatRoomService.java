package com.deundeun.chat.service;

import com.deundeun.chat.domain.ChatRoom;
import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.dto.response.ChallengeChatRoomResponse;
import com.deundeun.chat.dto.response.DirectChatRoomResponse;
import com.deundeun.chat.mapper.ChatMessageMapper;
import com.deundeun.chat.mapper.ChatRoomMapper;
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
public class ChatRoomService {

    private final ChatRoomMemberFinder chatRoomMemberFinder;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatRoomMemberMapper chatRoomMemberMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Transactional
    public ChatRoom createChallengeRoom(Long challengeId) {
        ChatRoom room = ChatRoom.createChallengeRoom(challengeId);

        chatRoomMapper.insert(room);
        log.info("[Chat] 챌린지 채팅방 생성 완료: challengeId={}, chatRoomId={}", challengeId, room.getId());

        return room;
    }

    @Transactional
    public DirectChatRoomResponse createOrGetDirectRoom(Long userId1, Long userId2) {
        validateNotSelfChat(userId1, userId2);

        String directChatKey = ChatRoom.buildDirectChatKey(userId1, userId2);

        return chatRoomMapper.findByDirectChatKey(directChatKey)
            .map(room -> {
                log.debug("[Chat] 기존 1:1 채팅방 조회: chatRoomId={}, directChatKey={}", room.getId(), directChatKey);
                return DirectChatRoomResponse.of(room, false);
            })
            .orElseGet(() -> createDirectRoom(directChatKey, userId1, userId2));
    }

    @Transactional(readOnly = true)
    public ChallengeChatRoomResponse getChallengeRoom(Long challengeId, Long userId) {
        ChatRoom room = getChatRoomByChallengeId(challengeId);
        ChatRoomMember member = chatRoomMemberFinder.findMember(room.getId(), userId);

        int memberCount = chatRoomMemberMapper.findActiveByChatRoomId(room.getId()).size();
        int unreadCount = chatMessageMapper.countAfterId(room.getId(), member.getLastReadMessageId());

        return ChallengeChatRoomResponse.of(room, memberCount, unreadCount, member);
    }

    private DirectChatRoomResponse createDirectRoom(String directChatKey, Long userId1, Long userId2) {
        ChatRoom room = ChatRoom.createDirectRoom(directChatKey);

        try {
            chatRoomMapper.insert(room);
        } catch (DuplicateKeyException e) {
            log.debug("[Chat] 동시 요청으로 인한 1:1 채팅방 중복 생성 시도, 기존 방 재조회: directChatKey={}", directChatKey);
            ChatRoom existingRoom = chatRoomMapper.findByDirectChatKey(directChatKey)
                .orElseThrow(() -> e);

            return DirectChatRoomResponse.of(existingRoom, false);
        }

        chatRoomMemberMapper.insert(ChatRoomMember.join(room.getId(), userId1));
        chatRoomMemberMapper.insert(ChatRoomMember.join(room.getId(), userId2));
        log.info("[Chat] 1:1 채팅방 생성 완료: chatRoomId={}, directChatKey={}", room.getId(), directChatKey);

        return DirectChatRoomResponse.of(room, true);
    }

    private ChatRoom getChatRoomByChallengeId(Long challengeId) {
        return chatRoomMapper.findByChallengeId(challengeId)
            .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private void validateNotSelfChat(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            log.debug("[Chat] 자기 자신과의 1:1 채팅방 생성 시도: userId={}", userId1);
            throw new ApiException(ErrorCode.CHAT_ROOM_SELF_CHAT_NOT_ALLOWED);
        }
    }
}
