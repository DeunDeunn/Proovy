package com.deundeun.chat.service;

import com.deundeun.chat.domain.ChatRoom;
import com.deundeun.chat.mapper.ChatRoomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;

    @Transactional
    public ChatRoom createChallengeRoom(Long challengeId) {
        ChatRoom room = ChatRoom.createChallengeRoom(challengeId);

        chatRoomMapper.insert(room);
        log.info("[Chat] 챌린지 채팅방 생성 완료: challengeId={}, chatRoomId={}", challengeId, room.getId());

        return room;
    }
}
