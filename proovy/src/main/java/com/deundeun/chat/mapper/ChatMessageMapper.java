package com.deundeun.chat.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deundeun.chat.domain.ChatMessage;
import com.deundeun.chat.dto.RoomReadCursor;
import com.deundeun.chat.dto.RoomUnreadCount;

@Mapper
public interface ChatMessageMapper {

    void insert(ChatMessage message);

    Optional<ChatMessage> findById(@Param("id") Long id);

    int delete(ChatMessage message);

    List<ChatMessage> findLatestByChatRoomId(@Param("chatRoomId") Long chatRoomId, @Param("limit") int limit);

    List<ChatMessage> findByChatRoomIdBeforeId(@Param("chatRoomId") Long chatRoomId,
                                                @Param("beforeId") Long beforeId,
                                                @Param("limit") int limit);

    int countAfterId(@Param("chatRoomId") Long chatRoomId, @Param("afterId") Long afterId);

    List<RoomUnreadCount> countUnreadByRooms(@Param("cursors") List<RoomReadCursor> cursors);
}
