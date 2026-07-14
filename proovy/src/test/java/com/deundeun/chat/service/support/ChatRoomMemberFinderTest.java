package com.deundeun.chat.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.chat.domain.ChatRoom;
import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.mapper.ChatRoomMapper;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class ChatRoomMemberFinderTest {

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @Mock
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @InjectMocks
    private ChatRoomMemberFinder chatRoomMemberFinder;

    @Test
    void findMember_roomNotFound_throwsNotFound() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        when(chatRoomMapper.findById(chatRoomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomMemberFinder.findMember(chatRoomId, userId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    void findMember_activeMember_returnsMember() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, userId);
        when(chatRoomMapper.findById(chatRoomId)).thenReturn(Optional.of(ChatRoom.createChallengeRoom(100L)));
        when(chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));

        ChatRoomMember result = chatRoomMemberFinder.findMember(chatRoomId, userId);

        assertThat(result).isEqualTo(member);
    }

    @Test
    void findMember_notAMember_throwsForbidden() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        when(chatRoomMapper.findById(chatRoomId)).thenReturn(Optional.of(ChatRoom.createChallengeRoom(100L)));
        when(chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomMemberFinder.findMember(chatRoomId, userId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN);
    }

    @Test
    void findMember_leftMember_throwsForbidden() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, userId);
        ReflectionTestUtils.setField(member, "leftAt", LocalDateTime.now());
        when(chatRoomMapper.findById(chatRoomId)).thenReturn(Optional.of(ChatRoom.createChallengeRoom(100L)));
        when(chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() -> chatRoomMemberFinder.findMember(chatRoomId, userId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN);
    }
}
