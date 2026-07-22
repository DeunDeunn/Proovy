package com.deundeun.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.auth.mapper.UserVerificationMapper;
import com.deundeun.chat.domain.ChatMessage;
import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.dto.response.ChatRoomMemberResponse;
import com.deundeun.chat.dto.response.ChatRoomReadResponse;
import com.deundeun.chat.mapper.ChatMessageMapper;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.chat.service.support.ChatRoomMemberFinder;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;

@DisplayName("ChatRoomMemberService")
@ExtendWith(MockitoExtension.class)
class ChatRoomMemberServiceTest {

    @Mock
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserVerificationMapper userVerificationMapper;

    @Mock
    private ChatRoomMemberFinder chatRoomMemberFinder;

    @InjectMocks
    private ChatRoomMemberService chatRoomMemberService;

    @Test
    @DisplayName("채팅방의 최신 메시지까지 읽음 처리하고 변경 내용을 저장한다")
    void markAsRead_updatesMemberAndPersists() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        Long lastReadMessageId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, userId);
        ChatMessage latestMessage = ChatMessage.create(chatRoomId, userId, "hi", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(latestMessage, "id", lastReadMessageId);
        when(chatRoomMemberFinder.findMember(chatRoomId, userId)).thenReturn(member);
        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 1)).thenReturn(List.of(latestMessage));
        when(chatRoomMemberMapper.updateLastRead(member)).thenReturn(1);

        ChatRoomReadResponse response = chatRoomMemberService.markAsRead(chatRoomId, userId);

        assertThat(response.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.lastReadMessageId()).isEqualTo(lastReadMessageId);

        ArgumentCaptor<ChatRoomMember> captor = ArgumentCaptor.forClass(ChatRoomMember.class);
        verify(chatRoomMemberMapper).updateLastRead(captor.capture());
        assertThat(captor.getValue()).isSameAs(member);
    }

    @Test
    @DisplayName("메시지가 없으면 읽음 커서를 갱신하지 않는다")
    void markAsRead_noMessages_keepsLastReadMessageIdNull() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, userId);
        when(chatRoomMemberFinder.findMember(chatRoomId, userId)).thenReturn(member);
        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 1)).thenReturn(List.of());

        ChatRoomReadResponse response = chatRoomMemberService.markAsRead(chatRoomId, userId);

        assertThat(response.lastReadMessageId()).isNull();
        assertThat(response.lastReadAt()).isNull();
        verify(chatRoomMemberMapper, never()).updateLastRead(member);
    }

    @Test
    @DisplayName("이미 최신 메시지까지 읽었으면 읽음 커서를 갱신하지 않는다")
    void markAsRead_alreadyReadLatestMessage_doesNotMoveCursor() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, userId);
        ReflectionTestUtils.setField(member, "lastReadMessageId", 20L);
        ChatMessage latestMessage = ChatMessage.create(chatRoomId, userId, "hi", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(latestMessage, "id", 20L);
        when(chatRoomMemberFinder.findMember(chatRoomId, userId)).thenReturn(member);
        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 1)).thenReturn(List.of(latestMessage));

        ChatRoomReadResponse response = chatRoomMemberService.markAsRead(chatRoomId, userId);

        assertThat(response.lastReadMessageId()).isEqualTo(20L);
        assertThat(response.lastReadAt()).isNull();
        verify(chatRoomMemberMapper, never()).updateLastRead(member);
    }

    @Test
    @DisplayName("요청자가 방 멤버면 활성 참여자 목록을 뱃지 승인 여부와 함께 반환한다")
    void getMembers_requesterIsMember_returnsActiveMembersWithBadgeInfo() {
        Long chatRoomId = 1L;
        Long requesterId = 10L;
        Long otherUserId = 20L;
        ChatRoomMember requesterMember = ChatRoomMember.join(chatRoomId, requesterId);
        ChatRoomMember otherMember = ChatRoomMember.join(chatRoomId, otherUserId);
        User requester = User.builder().id(requesterId).nickname("민기").profileImageUrl("url1").build();
        User other = User.builder().id(otherUserId).nickname("재영").profileImageUrl("url2").build();

        when(chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, requesterId))
            .thenReturn(Optional.of(requesterMember));
        when(chatRoomMemberMapper.findActiveByChatRoomId(chatRoomId))
            .thenReturn(List.of(requesterMember, otherMember));
        when(userMapper.findByIds(List.of(requesterId, otherUserId))).thenReturn(List.of(requester, other));
        when(userVerificationMapper.findApprovedUserIds(List.of(requesterId, otherUserId)))
            .thenReturn(List.of(otherUserId));

        List<ChatRoomMemberResponse> response = chatRoomMemberService.getMembers(chatRoomId, requesterId);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).userId()).isEqualTo(requesterId);
        assertThat(response.get(0).badgeApproved()).isFalse();
        assertThat(response.get(1).userId()).isEqualTo(otherUserId);
        assertThat(response.get(1).badgeApproved()).isTrue();
    }

    @Test
    @DisplayName("요청자가 방 멤버가 아니면 거부한다")
    void getMembers_requesterNotMember_throws() {
        Long chatRoomId = 1L;
        Long requesterId = 10L;
        when(chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, requesterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomMemberService.getMembers(chatRoomId, requesterId))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN);

        verify(chatRoomMemberMapper, never()).findActiveByChatRoomId(chatRoomId);
    }

    @Test
    @DisplayName("요청자가 방을 나간(비활성) 상태면 거부한다")
    void getMembers_requesterLeftRoom_throws() {
        Long chatRoomId = 1L;
        Long requesterId = 10L;
        ChatRoomMember leftMember = ChatRoomMember.join(chatRoomId, requesterId);
        leftMember.leave();
        when(chatRoomMemberMapper.findByChatRoomIdAndUserId(chatRoomId, requesterId))
            .thenReturn(Optional.of(leftMember));

        assertThatThrownBy(() -> chatRoomMemberService.getMembers(chatRoomId, requesterId))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN);

        verify(chatRoomMemberMapper, never()).findActiveByChatRoomId(chatRoomId);
    }
}
