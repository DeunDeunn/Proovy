package com.deundeun.chat.service.support;

import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.dto.RoomReadCursor;
import com.deundeun.chat.dto.RoomUnreadCount;
import com.deundeun.chat.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatUnreadCounter {

    private final ChatMessageMapper chatMessageMapper;

    public int count(ChatRoomMember member) {
        return count(member.getChatRoomId(), member.getLastReadMessageId());
    }

    public int count(Long chatRoomId, Long lastReadMessageId) {
        return chatMessageMapper.countAfterId(chatRoomId, lastReadMessageId);
    }

    public Map<Long, Integer> countBatch(Map<Long, Long> lastReadMessageIdByChatRoomId) {
        if (lastReadMessageIdByChatRoomId.isEmpty()) {
            return Collections.emptyMap();
        }

        List<RoomReadCursor> cursors = lastReadMessageIdByChatRoomId.entrySet().stream()
            .map(entry -> new RoomReadCursor(entry.getKey(), entry.getValue()))
            .toList();

        return chatMessageMapper.countUnreadByRooms(cursors).stream()
            .collect(Collectors.toMap(RoomUnreadCount::chatRoomId, RoomUnreadCount::unreadCount));
    }
}
