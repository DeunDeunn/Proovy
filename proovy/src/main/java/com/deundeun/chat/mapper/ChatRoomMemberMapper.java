package com.deundeun.chat.mapper;

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

    void updateLastRead(ChatRoomMember member);

    void leave(ChatRoomMember member);

    void rejoin(ChatRoomMember member);
}
