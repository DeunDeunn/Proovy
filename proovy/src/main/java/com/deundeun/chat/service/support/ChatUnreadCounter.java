package com.deundeun.chat.service.support;

import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatUnreadCounter {

    private final ChatMessageMapper chatMessageMapper;

    public int count(ChatRoomMember member) {
        return chatMessageMapper.countAfterId(member.getChatRoomId(), member.getLastReadMessageId());
    }
}
