package com.deundeun.chat.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deundeun.chat.domain.ChatRoomMember;

@Mapper
public interface ChatRoomMemberMapper {

    void insert(ChatRoomMember member);

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(@Param("chatRoomId") Long chatRoomId,
                                                        @Param("userId") Long userId);

    List<ChatRoomMember> findActiveByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    void updateLastRead(@Param("chatRoomId") Long chatRoomId,
                         @Param("userId") Long userId,
                         @Param("lastReadMessageId") Long lastReadMessageId,
                         @Param("lastReadAt") LocalDateTime lastReadAt);

    void leave(@Param("chatRoomId") Long chatRoomId,
               @Param("userId") Long userId,
               @Param("leftAt") LocalDateTime leftAt);

    void rejoin(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}
