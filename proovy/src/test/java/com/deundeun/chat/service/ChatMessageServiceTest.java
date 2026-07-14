package com.deundeun.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.certification.dto.chat.SharedCertificationInfo;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.chat.domain.ChatAttachment;
import com.deundeun.chat.domain.ChatFileType;
import com.deundeun.chat.domain.ChatMessage;
import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatReferenceType;
import com.deundeun.chat.dto.response.ChatMessageListResponse;
import com.deundeun.chat.mapper.ChatAttachmentMapper;
import com.deundeun.chat.mapper.ChatMessageMapper;
import com.deundeun.chat.service.support.ChatRoomMemberFinder;

@DisplayName("ChatMessageService")
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private ChatAttachmentMapper chatAttachmentMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CertificationMapper certificationMapper;

    @Mock
    private ChatRoomMemberFinder chatRoomMemberFinder;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private static ChatMessage messageWithId(long id) {
        ChatMessage message = ChatMessage.create(1L, 20L, "hi", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    @Test
    @DisplayName("메시지 조회 전 채팅방 접근 권한을 검증한다")
    void getMessages_validatesAccessBeforeQuerying() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 31)).thenReturn(List.of());

        chatMessageService.getMessages(chatRoomId, userId, null, 30);

        verify(chatRoomMemberFinder).validateMember(chatRoomId, userId);
    }

    @Test
    @DisplayName("beforeMessageId가 없으면 최신 메시지부터 조회한다")
    void getMessages_noBeforeMessageId_fetchesLatest() {
        Long chatRoomId = 1L;
        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 31)).thenReturn(List.of());

        chatMessageService.getMessages(chatRoomId, 10L, null, 30);

        verify(chatMessageMapper).findLatestByChatRoomId(chatRoomId, 31);
    }

    @Test
    @DisplayName("beforeMessageId가 있으면 그 이전 메시지를 조회한다")
    void getMessages_withBeforeMessageId_fetchesBeforeId() {
        Long chatRoomId = 1L;
        when(chatMessageMapper.findByChatRoomIdBeforeId(chatRoomId, 50L, 31)).thenReturn(List.of());

        chatMessageService.getMessages(chatRoomId, 10L, 50L, 30);

        verify(chatMessageMapper).findByChatRoomIdBeforeId(chatRoomId, 50L, 31);
    }

    @Test
    @DisplayName("size만큼만 조회되면 다음 페이지가 없다고 판단한다")
    void getMessages_exactlySizeMessages_hasNextFalse() {
        Long chatRoomId = 1L;
        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 31)).thenReturn(List.of(messageWithId(100L)));
        when(userMapper.findByIds(any())).thenReturn(List.of());
        when(chatAttachmentMapper.findByMessageIds(any())).thenReturn(List.of());

        ChatMessageListResponse response = chatMessageService.getMessages(chatRoomId, 10L, null, 30);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.content()).hasSize(1);
    }

    @Test
    @DisplayName("size보다 하나 더 조회되면 다음 페이지가 있다고 판단하고 초과분을 잘라낸다")
    void getMessages_moreThanSizeMessages_hasNextTrueAndTrimsExtra() {
        Long chatRoomId = 1L;
        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 2))
            .thenReturn(List.of(messageWithId(100L), messageWithId(99L)));
        when(userMapper.findByIds(any())).thenReturn(List.of());
        when(chatAttachmentMapper.findByMessageIds(any())).thenReturn(List.of());

        ChatMessageListResponse response = chatMessageService.getMessages(chatRoomId, 10L, null, 1);

        assertThat(response.content()).hasSize(1);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(100L);
    }

    @Test
    @DisplayName("메시지가 없으면 발신자/첨부파일 조회를 건너뛴다")
    void getMessages_noMessages_skipsSenderAndAttachmentLookup() {
        Long chatRoomId = 1L;
        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 31)).thenReturn(List.of());

        ChatMessageListResponse response = chatMessageService.getMessages(chatRoomId, 10L, null, 30);

        assertThat(response.content()).isEmpty();
        verify(userMapper, never()).findByIds(any());
        verify(chatAttachmentMapper, never()).findByMessageIds(any());
    }

    @Test
    @DisplayName("발신자 정보와 첨부파일을 응답에 조립한다")
    void getMessages_assemblesSenderAndAttachmentInfo() {
        Long chatRoomId = 1L;
        Long userId = 10L;
        Long senderId = 20L;
        ChatMessage message = ChatMessage.create(chatRoomId, senderId, "hi", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(message, "id", 100L);
        User sender = User.builder().id(senderId).nickname("민기").profileImageUrl("url").build();
        ChatAttachment attachment = ChatAttachment.upload(senderId, "file-url", "file.png", ChatFileType.IMAGE, 100L);
        ReflectionTestUtils.setField(attachment, "messageId", 100L);

        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 31)).thenReturn(List.of(message));
        when(userMapper.findByIds(List.of(senderId))).thenReturn(List.of(sender));
        when(chatAttachmentMapper.findByMessageIds(List.of(100L))).thenReturn(List.of(attachment));

        ChatMessageListResponse response = chatMessageService.getMessages(chatRoomId, userId, null, 30);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).senderNickname()).isEqualTo("민기");
        assertThat(response.content().get(0).attachments()).hasSize(1);
        verify(certificationMapper, never()).findSharedCertifications(any());
    }

    @Test
    @DisplayName("인증글 공유 메시지는 공유 인증글 정보를 조회한다")
    void getMessages_certificationShareMessage_looksUpSharedCertification() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        Long postId = 50L;
        ChatMessage message = ChatMessage.create(chatRoomId, senderId, null, ChatMessageType.CERTIFICATION_SHARE,
            ChatReferenceType.CHALLENGE_CERTIFICATION, postId);
        ReflectionTestUtils.setField(message, "id", 100L);
        SharedCertificationInfo info = new SharedCertificationInfo(
            postId, 7L, "매일 아침 7시 기상", senderId, "민기", "thumb-url", message.getCreatedAt());

        when(chatMessageMapper.findLatestByChatRoomId(chatRoomId, 31)).thenReturn(List.of(message));
        when(userMapper.findByIds(List.of(senderId))).thenReturn(List.of());
        when(chatAttachmentMapper.findByMessageIds(List.of(100L))).thenReturn(List.of());
        when(certificationMapper.findSharedCertifications(List.of(postId))).thenReturn(List.of(info));

        ChatMessageListResponse response = chatMessageService.getMessages(chatRoomId, 10L, null, 30);

        assertThat(response.content().get(0).sharedCertification()).isNotNull();
        assertThat(response.content().get(0).sharedCertification().challengeTitle()).isEqualTo("매일 아침 7시 기상");
    }
}
