package com.deundeun.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.chat.domain.ChatRoom;
import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.domain.ChatRoomType;
import com.deundeun.chat.dto.response.ChallengeChatRoomResponse;
import com.deundeun.chat.dto.response.DirectChatRoomResponse;
import com.deundeun.chat.mapper.ChatMessageMapper;
import com.deundeun.chat.mapper.ChatRoomMapper;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.chat.service.validator.ChatRoomMemberValidator;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @Mock
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private ChatRoomMemberValidator chatRoomMemberValidator;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Test
    void createChallengeRoom_success() {
        Long challengeId = 100L;

        ChatRoom room = chatRoomService.createChallengeRoom(challengeId);

        assertThat(room.getChallengeId()).isEqualTo(challengeId);
        assertThat(room.getType()).isEqualTo(ChatRoomType.CHALLENGE);
        verify(chatRoomMapper).insert(room);
    }

    @Test
    void createOrGetDirectRoom_existingRoom_reusesRoom() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        ChatRoom existingRoom = ChatRoom.createDirectRoom(ChatRoom.buildDirectChatKey(userId1, userId2));
        ReflectionTestUtils.setField(existingRoom, "id", 10L);
        when(chatRoomMapper.findByDirectChatKey("1:2")).thenReturn(Optional.of(existingRoom));

        DirectChatRoomResponse response = chatRoomService.createOrGetDirectRoom(userId1, userId2);

        assertThat(response.chatRoomId()).isEqualTo(10L);
        assertThat(response.created()).isFalse();
        verify(chatRoomMapper, never()).insert(any());
        verify(chatRoomMemberMapper, never()).insert(any());
    }

    @Test
    void createOrGetDirectRoom_noExistingRoom_createsRoomAndRegistersBothMembers() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        when(chatRoomMapper.findByDirectChatKey("1:2")).thenReturn(Optional.empty());

        DirectChatRoomResponse response = chatRoomService.createOrGetDirectRoom(userId1, userId2);

        assertThat(response.created()).isTrue();
        assertThat(response.directChatKey()).isEqualTo("1:2");

        ArgumentCaptor<ChatRoomMember> captor = ArgumentCaptor.forClass(ChatRoomMember.class);
        verify(chatRoomMemberMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(ChatRoomMember::getUserId)
            .containsExactlyInAnyOrder(userId1, userId2);
    }

    @Test
    void createOrGetDirectRoom_selfChat_throwsException() {
        Long userId = 1L;

        assertThatThrownBy(() -> chatRoomService.createOrGetDirectRoom(userId, userId))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ROOM_SELF_CHAT_NOT_ALLOWED);

        verify(chatRoomMapper, never()).findByDirectChatKey(any());
        verify(chatRoomMapper, never()).insert(any());
    }

    @Test
    void createOrGetDirectRoom_duplicateKeyOnInsert_fallsBackToExistingRoom() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        ChatRoom existingRoom = ChatRoom.createDirectRoom("1:2");
        ReflectionTestUtils.setField(existingRoom, "id", 20L);

        when(chatRoomMapper.findByDirectChatKey("1:2"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(existingRoom));
        doThrow(new DuplicateKeyException("duplicate direct_chat_key"))
            .when(chatRoomMapper).insert(any(ChatRoom.class));

        DirectChatRoomResponse response = chatRoomService.createOrGetDirectRoom(userId1, userId2);

        assertThat(response.chatRoomId()).isEqualTo(20L);
        assertThat(response.created()).isFalse();
        verify(chatRoomMemberMapper, never()).insert(any());
    }

    @Test
    void getChallengeRoom_roomNotFound_throwsException() {
        Long challengeId = 100L;
        Long userId = 1L;
        when(chatRoomMapper.findByChallengeId(challengeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.getChallengeRoom(challengeId, userId))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);

        verify(chatRoomMemberValidator, never()).findMember(any(), any());
    }

    @Test
    void getChallengeRoom_notMember_propagatesValidatorException() {
        Long challengeId = 100L;
        Long userId = 1L;
        ChatRoom room = ChatRoom.createChallengeRoom(challengeId);
        ReflectionTestUtils.setField(room, "id", 5L);
        when(chatRoomMapper.findByChallengeId(challengeId)).thenReturn(Optional.of(room));
        when(chatRoomMemberValidator.findMember(5L, userId))
            .thenThrow(new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN));

        assertThatThrownBy(() -> chatRoomService.getChallengeRoom(challengeId, userId))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN);
    }

    @Test
    void getChallengeRoom_success_returnsFullData() {
        Long challengeId = 100L;
        Long userId = 1L;
        ChatRoom room = ChatRoom.createChallengeRoom(challengeId);
        ReflectionTestUtils.setField(room, "id", 5L);

        ChatRoomMember member = ChatRoomMember.join(5L, userId);
        ReflectionTestUtils.setField(member, "lastReadMessageId", 20L);

        when(chatRoomMapper.findByChallengeId(challengeId)).thenReturn(Optional.of(room));
        when(chatRoomMemberValidator.findMember(5L, userId)).thenReturn(member);
        when(chatRoomMemberMapper.findActiveByChatRoomId(5L))
            .thenReturn(List.of(ChatRoomMember.join(5L, userId), ChatRoomMember.join(5L, 2L)));
        when(chatMessageMapper.countAfterId(5L, 20L)).thenReturn(3);

        ChallengeChatRoomResponse response = chatRoomService.getChallengeRoom(challengeId, userId);

        assertThat(response.chatRoomId()).isEqualTo(5L);
        assertThat(response.memberCount()).isEqualTo(2);
        assertThat(response.unreadCount()).isEqualTo(3);
        assertThat(response.lastReadMessageId()).isEqualTo(20L);
    }
}
