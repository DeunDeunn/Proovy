package com.deundeun.chat.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deundeun.chat.domain.ChatRoom;

@Mapper
public interface ChatRoomMapper {

    void insert(ChatRoom room);

    Optional<ChatRoom> findById(@Param("chatRoomId") Long chatRoomId);

    Optional<ChatRoom> findByChallengeId(@Param("challengeId") Long challengeId);

    Optional<ChatRoom> findByDirectChatKey(@Param("directChatKey") String directChatKey);
}
