package com.deundeun.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.chat.domain.ChatMessage;
import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.dto.response.ChatRoomReadResponse;
import com.deundeun.chat.mapper.ChatMessageMapper;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.chat.service.support.ChatRoomMemberFinder;

@DisplayName("ChatRoomMemberService")
@ExtendWith(MockitoExtension.class)
class ChatRoomMemberServiceTest {

    @Mock
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

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
}
