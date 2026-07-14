package com.deundeun.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
    void markAsRead_updatesMemberAndPersists() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        Long lastReadMessageId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, userId);
        ChatMessage latestMessage = ChatMessage.create(chatRoomId, userId, "hi", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(latestMessage, "id", lastReadMessageId);
        when(chatRoomMemberFinder.findMember(chatRoomId, userId)).thenReturn(member);
        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 1)).thenReturn(List.of(latestMessage));

        ChatRoomReadResponse response = chatRoomMemberService.markAsRead(chatRoomId, userId);

        assertThat(response.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.lastReadMessageId()).isEqualTo(lastReadMessageId);

        ArgumentCaptor<ChatRoomMember> captor = ArgumentCaptor.forClass(ChatRoomMember.class);
        verify(chatRoomMemberMapper).updateLastRead(captor.capture());
        assertThat(captor.getValue()).isSameAs(member);
    }

    @Test
    void markAsRead_noMessages_keepsLastReadMessageIdNull() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, userId);
        when(chatRoomMemberFinder.findMember(chatRoomId, userId)).thenReturn(member);
        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 1)).thenReturn(List.of());

        ChatRoomReadResponse response = chatRoomMemberService.markAsRead(chatRoomId, userId);

        assertThat(response.lastReadMessageId()).isNull();
        assertThat(response.lastReadAt()).isNotNull();
        verify(chatRoomMemberMapper).updateLastRead(member);
    }
}
