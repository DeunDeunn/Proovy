package com.deundeun.chat.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.auth.domain.User;
import com.deundeun.chat.domain.ChatAttachment;
import com.deundeun.chat.domain.ChatFileType;
import com.deundeun.chat.domain.ChatMessage;
import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatReferenceType;

@DisplayName("ChatMessageResponse")
class ChatMessageResponseTest {

    @Test
    @DisplayName("삭제되지 않은 메시지는 모든 필드를 포함한다")
    void of_activeMessage_includesAllFields() {
        ChatMessage message = ChatMessage.create(1L, 10L, "안녕하세요", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(message, "id", 100L);
        User sender = User.builder().id(10L).nickname("민기").profileImageUrl("https://example.com/p.png").build();
        ChatAttachment attachment = ChatAttachment.create(100L, 10L, "https://example.com/f.png", "f.png", ChatFileType.IMAGE, 1024L);
        ReflectionTestUtils.setField(attachment, "id", 5L);

        ChatMessageResponse response = ChatMessageResponse.of(message, sender, List.of(attachment), null, true, true);

        assertThat(response.messageId()).isEqualTo(100L);
        assertThat(response.chatRoomId()).isEqualTo(1L);
        assertThat(response.senderId()).isEqualTo(10L);
        assertThat(response.senderNickname()).isEqualTo("민기");
        assertThat(response.senderProfileImage()).isEqualTo("https://example.com/p.png");
        assertThat(response.senderBadgeApproved()).isTrue();
        assertThat(response.content()).isEqualTo("안녕하세요");
        assertThat(response.attachments()).hasSize(1);
        assertThat(response.read()).isTrue();
        assertThat(response.deletedAt()).isNull();
    }

    @Test
    @DisplayName("삭제된 메시지는 content/참조/공유정보/첨부파일을 비운다")
    void of_deletedMessage_nullsContentReferenceAndAttachments() {
        ChatMessage message = ChatMessage.create(1L, 10L, "삭제될 메시지", ChatMessageType.CERTIFICATION_SHARE,
            ChatReferenceType.CHALLENGE_CERTIFICATION, 50L);
        ReflectionTestUtils.setField(message, "id", 100L);
        ReflectionTestUtils.setField(message, "deletedAt", LocalDateTime.now());
        ChatAttachment attachment = ChatAttachment.create(100L, 10L, "https://example.com/f.png", "f.png", ChatFileType.IMAGE, 1024L);
        SharedCertificationResponse sharedCertification = new SharedCertificationResponse(
            50L, 7L, "매일 아침 7시 기상", 10L, "민기", "https://example.com/thumb.png", LocalDateTime.now());

        ChatMessageResponse response = ChatMessageResponse.of(message, null, List.of(attachment), sharedCertification, false, false);

        assertThat(response.content()).isNull();
        assertThat(response.referenceType()).isNull();
        assertThat(response.referenceId()).isNull();
        assertThat(response.sharedCertification()).isNull();
        assertThat(response.attachments()).isEmpty();
        assertThat(response.deletedAt()).isNotNull();
    }

    @Test
    @DisplayName("발신자가 없으면 닉네임/프로필 이미지는 null이다")
    void of_senderNull_leavesNicknameAndProfileNull() {
        ChatMessage message = ChatMessage.create(1L, 10L, "hi", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(message, "id", 100L);

        ChatMessageResponse response = ChatMessageResponse.of(message, null, List.of(), null, true, false);

        assertThat(response.senderNickname()).isNull();
        assertThat(response.senderProfileImage()).isNull();
        assertThat(response.senderBadgeApproved()).isTrue();
    }
}
