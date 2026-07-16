package com.deundeun.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.certification.dto.chat.SharedCertificationInfo;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.chat.domain.ChatAttachment;
import com.deundeun.chat.domain.ChatFileType;
import com.deundeun.chat.domain.ChatMessage;
import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatReferenceType;
import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.dto.request.ChatMessageSendRequest;
import com.deundeun.chat.dto.response.ChatMessageListResponse;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import com.deundeun.chat.mapper.ChatAttachmentMapper;
import com.deundeun.chat.mapper.ChatMessageMapper;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.chat.service.support.ChatCertificationShareValidator;
import com.deundeun.chat.service.support.ChatRoomMemberFinder;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.file.FileCategory;
import com.deundeun.global.file.TransactionalFileUploader;

@DisplayName("ChatMessageService")
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private ChatAttachmentMapper chatAttachmentMapper;

    @Mock
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CertificationMapper certificationMapper;

    @Mock
    private ChatRoomMemberFinder chatRoomMemberFinder;

    @Mock
    private TransactionalFileUploader fileUploader;

    @Mock
    private ChatCertificationShareValidator certificationShareValidator;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("TEXT 메시지를 저장하고 발신자의 읽음커서를 갱신한다")
    void send_textMessage_savesAndAdvancesSenderReadCursor() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(ChatMessageType.TEXT, "안녕하세요", null, null);
        User sender = User.builder().id(senderId).nickname("민기").profileImageUrl("url").build();

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);
        stubMessageInsertAssignsId(100L);
        when(userMapper.findById(senderId)).thenReturn(sender);

        ChatMessageResponse response = chatMessageService.send(chatRoomId, senderId, request, null);

        assertThat(response.messageId()).isEqualTo(100L);
        assertThat(response.messageType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(response.content()).isEqualTo("안녕하세요");
        assertThat(response.attachments()).isEmpty();
        assertThat(member.getLastReadMessageId()).isEqualTo(100L);
        verify(chatRoomMemberMapper).updateLastRead(member);
    }

    @Test
    @DisplayName("채팅방 참여자가 아니면 메시지를 저장하지 않는다")
    void send_notMember_doesNotSaveMessage() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatMessageSendRequest request = new ChatMessageSendRequest(ChatMessageType.TEXT, "안녕하세요", null, null);

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId))
            .thenThrow(new ApiException(ErrorCode.CHAT_ROOM_FORBIDDEN));

        assertThatThrownBy(() -> chatMessageService.send(chatRoomId, senderId, request, null))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN);

        verify(chatMessageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("메시지 타입이 없으면 거부한다")
    void send_nullMessageType_throws() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(null, null, null, null);

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);

        assertThatThrownBy(() -> chatMessageService.send(chatRoomId, senderId, request, null))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_INVALID_MESSAGE_TYPE);

        verify(chatMessageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("TEXT 메시지인데 content가 비어있으면 거부한다")
    void send_blankContent_throws() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(ChatMessageType.TEXT, "   ", null, null);

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);

        assertThatThrownBy(() -> chatMessageService.send(chatRoomId, senderId, request, null))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED);

        verify(chatMessageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("메시지 내용이 1000자를 초과하면 거부한다")
    void send_contentTooLong_throws() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(ChatMessageType.TEXT, "a".repeat(1001), null, null);

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);

        assertThatThrownBy(() -> chatMessageService.send(chatRoomId, senderId, request, null))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG);

        verify(chatMessageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("메시지 내용이 정확히 1000자면 저장에 성공한다")
    void send_contentExactlyMaxLength_saves() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        String maxLengthContent = "a".repeat(1000);
        ChatMessageSendRequest request = new ChatMessageSendRequest(ChatMessageType.TEXT, maxLengthContent, null, null);

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);
        stubMessageInsertAssignsId(100L);
        when(userMapper.findById(senderId)).thenReturn(null);

        ChatMessageResponse response = chatMessageService.send(chatRoomId, senderId, request, null);

        assertThat(response.content()).isEqualTo(maxLengthContent);
        verify(chatMessageMapper).insert(any());
    }

    @Test
    @DisplayName("TEXT 메시지에 파일이 같이 오면 거부한다")
    void send_textWithFile_throws() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(ChatMessageType.TEXT, "안녕하세요", null, null);
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "content".getBytes());

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);

        assertThatThrownBy(() -> chatMessageService.send(chatRoomId, senderId, request, file))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ATTACHMENT_NOT_ALLOWED);

        verify(chatMessageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("IMAGE 메시지인데 파일이 없으면 거부한다")
    void send_imageWithoutFile_throws() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(ChatMessageType.IMAGE, null, null, null);

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);

        assertThatThrownBy(() -> chatMessageService.send(chatRoomId, senderId, request, null))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_ATTACHMENT_REQUIRED);

        verify(chatMessageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("IMAGE 메시지는 파일을 업로드하고 첨부파일과 함께 응답한다")
    void send_imageWithFile_uploadsAndReturnsAttachment() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(ChatMessageType.IMAGE, null, null, null);
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "content".getBytes());

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);
        stubMessageInsertAssignsId(100L);
        when(fileUploader.upload(file, FileCategory.CHAT)).thenReturn("https://bucket.s3.region.amazonaws.com/chat/uuid.png");
        when(userMapper.findById(senderId)).thenReturn(null);

        ChatMessageResponse response = chatMessageService.send(chatRoomId, senderId, request, file);

        assertThat(response.attachments()).hasSize(1);
        assertThat(response.attachments().get(0).fileUrl()).isEqualTo("https://bucket.s3.region.amazonaws.com/chat/uuid.png");
        assertThat(response.attachments().get(0).fileType()).isEqualTo(ChatFileType.IMAGE);
        verify(chatAttachmentMapper).insert(any(ChatAttachment.class));
    }

    @Test
    @DisplayName("인증글 공유 메시지는 content를 고정 문자열로 저장하고 공유 정보를 응답에 포함한다")
    void send_certificationShare_savesWithFixedContentAndSharedCertification() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        Long postId = 50L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(
            ChatMessageType.CERTIFICATION_SHARE, "무시될 내용", ChatReferenceType.CHALLENGE_CERTIFICATION, postId);
        SharedCertificationInfo info = new SharedCertificationInfo(
            postId, 7L, "매일 아침 7시 기상", senderId, "민기", "thumb-url", null);

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);
        stubMessageInsertAssignsId(100L);
        when(certificationMapper.findSharedCertifications(List.of(postId))).thenReturn(List.of(info));
        when(userMapper.findById(senderId)).thenReturn(null);

        ChatMessageResponse response = chatMessageService.send(chatRoomId, senderId, request, null);

        assertThat(response.content()).isEqualTo("인증 글 공유합니다!");
        assertThat(response.messageType()).isEqualTo(ChatMessageType.CERTIFICATION_SHARE);
        assertThat(response.referenceType()).isEqualTo(ChatReferenceType.CHALLENGE_CERTIFICATION);
        assertThat(response.referenceId()).isEqualTo(postId);
        assertThat(response.sharedCertification()).isNotNull();
        assertThat(response.sharedCertification().challengeTitle()).isEqualTo("매일 아침 7시 기상");
        verify(certificationShareValidator).validateShareable(postId, senderId);
    }

    @Test
    @DisplayName("인증글 공유 메시지인데 referenceId가 없으면 거부한다")
    void send_certificationShareWithoutReferenceId_throws() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(
            ChatMessageType.CERTIFICATION_SHARE, null, ChatReferenceType.CHALLENGE_CERTIFICATION, null);

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);

        assertThatThrownBy(() -> chatMessageService.send(chatRoomId, senderId, request, null))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_REFERENCE_REQUIRED);

        verify(chatMessageMapper, never()).insert(any());
        verify(certificationShareValidator, never()).validateShareable(any(), any());
    }

    @Test
    @DisplayName("인증글 공유 메시지인데 referenceType이 없으면 거부한다")
    void send_certificationShareWithoutReferenceType_throws() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(
            ChatMessageType.CERTIFICATION_SHARE, null, null, 50L);

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);

        assertThatThrownBy(() -> chatMessageService.send(chatRoomId, senderId, request, null))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.CHAT_REFERENCE_REQUIRED);

        verify(chatMessageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("공유할 수 없는 인증글이면 메시지를 저장하지 않는다")
    void send_certificationShareNotShareable_doesNotSaveMessage() {
        Long chatRoomId = 1L;
        Long senderId = 20L;
        Long postId = 50L;
        ChatRoomMember member = ChatRoomMember.join(chatRoomId, senderId);
        ChatMessageSendRequest request = new ChatMessageSendRequest(
            ChatMessageType.CERTIFICATION_SHARE, null, ChatReferenceType.CHALLENGE_CERTIFICATION, postId);

        when(chatRoomMemberFinder.findMember(chatRoomId, senderId)).thenReturn(member);
        doThrow(new ApiException(ErrorCode.POST_NOT_FOUND))
            .when(certificationShareValidator).validateShareable(postId, senderId);

        assertThatThrownBy(() -> chatMessageService.send(chatRoomId, senderId, request, null))
            .isInstanceOf(ApiException.class)
            .extracting(e -> ((ApiException) e).getErrorCode())
            .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(chatMessageMapper, never()).insert(any());
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
        ChatAttachment attachment = ChatAttachment.create(100L, senderId, "file-url", "file.png", ChatFileType.IMAGE, 100L);

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

    private static ChatMessage messageWithId(long id) {
        ChatMessage message = ChatMessage.create(1L, 20L, "hi", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private void stubMessageInsertAssignsId(long id) {
        doAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", id);
            return null;
        }).when(chatMessageMapper).insert(any());
    }
}
