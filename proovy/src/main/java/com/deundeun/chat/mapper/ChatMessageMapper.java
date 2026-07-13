package com.deundeun.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deundeun.chat.domain.ChatMessage;

@Mapper
public interface ChatMessageMapper {

    void insert(ChatMessage message);

    List<ChatMessage> findLatestByChatRoomId(@Param("chatRoomId") Long chatRoomId, @Param("limit") int limit);

    List<ChatMessage> findByChatRoomIdBeforeId(@Param("chatRoomId") Long chatRoomId,
                                                @Param("beforeId") Long beforeId,
                                                @Param("limit") int limit);
}
