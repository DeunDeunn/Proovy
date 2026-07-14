package com.deundeun.chat.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.mapper.ChatMessageMapper;

@ExtendWith(MockitoExtension.class)
class ChatUnreadCounterTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @InjectMocks
    private ChatUnreadCounter chatUnreadCounter;

    @Test
    void count_delegatesToChatMessageMapperWithMemberLastReadMessageId() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, userId);
        ReflectionTestUtils.setField(member, "lastReadMessageId", 20L);
        when(chatMessageMapper.countAfterId(chatRoomId, 20L)).thenReturn(3);

        int result = chatUnreadCounter.count(member);

        assertThat(result).isEqualTo(3);
    }
}
