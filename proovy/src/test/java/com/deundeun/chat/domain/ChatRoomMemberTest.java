package com.deundeun.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatRoomMemberTest {

    @Test
    void markRead_updatesLastReadMessageIdAndLastReadAt() {
        ChatRoomMember member = ChatRoomMember.join(1L, 10L);

        member.markRead(20L);

        assertThat(member.getLastReadMessageId()).isEqualTo(20L);
        assertThat(member.getLastReadAt()).isNotNull();
    }
}
