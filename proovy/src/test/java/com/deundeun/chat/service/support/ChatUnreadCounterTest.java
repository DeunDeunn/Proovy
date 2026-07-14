package com.deundeun.chat.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.dto.RoomReadCursor;
import com.deundeun.chat.dto.RoomUnreadCount;
import com.deundeun.chat.mapper.ChatMessageMapper;

@DisplayName("ChatUnreadCounter")
@ExtendWith(MockitoExtension.class)
class ChatUnreadCounterTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @InjectMocks
    private ChatUnreadCounter chatUnreadCounter;

    @Test
    @DisplayName("멤버의 lastReadMessageId 기준으로 안읽음 개수를 조회한다")
    void count_delegatesToChatMessageMapperWithMemberLastReadMessageId() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, userId);
        ReflectionTestUtils.setField(member, "lastReadMessageId", 20L);
        when(chatMessageMapper.countAfterId(chatRoomId, 20L)).thenReturn(3);

        int result = chatUnreadCounter.count(member);

        assertThat(result).isEqualTo(3);
    }

    @Test
    @DisplayName("빈 맵이면 쿼리 없이 빈 결과를 반환한다")
    void countBatch_emptyMap_returnsEmptyMapWithoutQuerying() {
        Map<Long, Integer> result = chatUnreadCounter.countBatch(Map.of());

        assertThat(result).isEmpty();
        verify(chatMessageMapper, never()).countUnreadByRooms(anyList());
    }

    @Test
    @DisplayName("커서 목록을 만들어 배치 조회하고 결과를 chatRoomId 기준으로 매핑한다")
    void countBatch_buildsCursorsAndMapsResultByChatRoomId() {
        Map<Long, Long> lastReadMessageIdByChatRoomId = Map.of(1L, 20L, 2L, 0L);
        when(chatMessageMapper.countUnreadByRooms(anyList()))
            .thenReturn(List.of(new RoomUnreadCount(1L, 3), new RoomUnreadCount(2L, 0)));

        Map<Long, Integer> result = chatUnreadCounter.countBatch(lastReadMessageIdByChatRoomId);

        assertThat(result).containsEntry(1L, 3).containsEntry(2L, 0);

        ArgumentCaptor<List<RoomReadCursor>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatMessageMapper).countUnreadByRooms(captor.capture());
        assertThat(captor.getValue())
            .containsExactlyInAnyOrder(new RoomReadCursor(1L, 20L), new RoomReadCursor(2L, 0L));
    }
}
