package com.deundeun.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;

@DisplayName("ChatRoomMember")
class ChatRoomMemberTest {

    @Test
    @DisplayName("읽음 커서를 최신 메시지 ID로 갱신하고 읽은 시각을 기록한다")
    void markRead_updatesLastReadMessageIdAndLastReadAt() {
        ChatRoomMember member = ChatRoomMember.join(1L, 10L);

        member.markRead(20L);

        assertThat(member.getLastReadMessageId()).isEqualTo(20L);
        assertThat(member.getLastReadAt()).isNotNull();
    }

    @Test
    @DisplayName("읽음 처리할 메시지 ID가 null이면 예외를 던진다")
    void markRead_nullLastReadMessageId_throwsException() {
        ChatRoomMember member = ChatRoomMember.join(1L, 10L);

        assertThatThrownBy(() -> member.markRead(null))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_INVALID_READ_CURSOR);
    }

    @Test
    @DisplayName("읽음 커서가 같거나 이전 메시지로 역행하면 예외를 던진다")
    void markRead_olderOrSameLastReadMessageId_throwsException() {
        ChatRoomMember member = ChatRoomMember.join(1L, 10L);
        ReflectionTestUtils.setField(member, "lastReadMessageId", 20L);

        assertThatThrownBy(() -> member.markRead(20L))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_INVALID_READ_CURSOR);

        assertThatThrownBy(() -> member.markRead(19L))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_INVALID_READ_CURSOR);
    }
}
