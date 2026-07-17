package com.deundeun.chat.service;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.certification.dto.chat.SharedCertificationInfo;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.chat.domain.*;
import com.deundeun.chat.dto.request.ChatMessageSendRequest;
import com.deundeun.chat.dto.response.ChatMessageDeleteResponse;
import com.deundeun.chat.dto.response.ChatMessageListResponse;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import com.deundeun.chat.dto.response.SharedCertificationResponse;
import com.deundeun.chat.mapper.ChatAttachmentMapper;
import com.deundeun.chat.mapper.ChatMessageMapper;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.chat.service.support.ChatCertificationShareValidator;
import com.deundeun.chat.service.support.ChatRoomMemberFinder;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.file.FileCategory;
import com.deundeun.global.file.TransactionalFileUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final String CERTIFICATION_SHARE_CONTENT = "인증 글 공유합니다!";

    private final ChatMessageMapper chatMessageMapper;
    private final ChatAttachmentMapper chatAttachmentMapper;
    private final ChatRoomMemberMapper chatRoomMemberMapper;
    private final UserMapper userMapper;
    private final CertificationMapper certificationMapper;
    private final ChatRoomMemberFinder chatRoomMemberFinder;
    private final TransactionalFileUploader fileUploader;
    private final ChatCertificationShareValidator certificationShareValidator;

    @Transactional
    public ChatMessageResponse send(Long chatRoomId, Long senderId, ChatMessageSendRequest request, MultipartFile file) {
        return doSend(chatRoomId, senderId, request, file);
    }

    @Transactional
    public ChatMessageResponse sendAttachment(Long chatRoomId, Long senderId, ChatMessageType messageType,
                                               String content, MultipartFile file) {
        if (messageType != ChatMessageType.IMAGE && messageType != ChatMessageType.FILE) {
            throw new ApiException(ErrorCode.CHAT_ATTACHMENT_ENDPOINT_TYPE_NOT_ALLOWED);
        }

        ChatMessageSendRequest request = new ChatMessageSendRequest(messageType, content, null, null);
        return doSend(chatRoomId, senderId, request, file);
    }

    private ChatMessageResponse doSend(Long chatRoomId, Long senderId, ChatMessageSendRequest request, MultipartFile file) {
        ChatRoomMember member = chatRoomMemberFinder.findMember(chatRoomId, senderId);
        ChatMessageType messageType = request.messageType();

        validateMessage(messageType, request.content(), request.referenceType(), request.referenceId(), file);

        if (messageType == ChatMessageType.CERTIFICATION_SHARE) {
            certificationShareValidator.validateShareable(request.referenceId(), senderId);
        }

        String content = resolveContent(messageType, request.content());
        ChatReferenceType referenceType = messageType == ChatMessageType.CERTIFICATION_SHARE ? request.referenceType() : null;
        Long referenceId = messageType == ChatMessageType.CERTIFICATION_SHARE ? request.referenceId() : null;

        ChatMessage message = ChatMessage.create(chatRoomId, senderId, content, messageType, referenceType, referenceId);
        chatMessageMapper.insert(message);

        List<ChatAttachment> attachments = List.of();
        if (file != null && !file.isEmpty()) {
            String url = fileUploader.upload(file, FileCategory.CHAT);
            ChatAttachment attachment = ChatAttachment.create(
                message.getId(), senderId, url, file.getOriginalFilename(), toChatFileType(messageType), file.getSize());
            chatAttachmentMapper.insert(attachment);
            attachments = List.of(attachment);
        }

        SharedCertificationResponse sharedCertification = messageType == ChatMessageType.CERTIFICATION_SHARE
            ? findSharedCertification(referenceId)
            : null;

        updateSenderReadCursor(member, message.getId());

        User sender = userMapper.findById(senderId);
        log.debug("[Chat] 메시지 전송 완료: chatRoomId={}, senderId={}, messageId={}, messageType={}",
            chatRoomId, senderId, message.getId(), messageType);

        return ChatMessageResponse.of(message, sender, attachments, sharedCertification);
    }

    @Transactional(readOnly = true)
    public ChatMessageListResponse getMessages(Long chatRoomId, Long userId, Long beforeMessageId, int size) {
        chatRoomMemberFinder.validateMember(chatRoomId, userId);

        List<ChatMessage> fetched = beforeMessageId == null
            ? chatMessageMapper.findLatestByChatRoomId(chatRoomId, size + 1)
            : chatMessageMapper.findByChatRoomIdBeforeId(chatRoomId, beforeMessageId, size + 1);

        boolean hasNext = fetched.size() > size;
        List<ChatMessage> messages = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = hasNext ? messages.get(messages.size() - 1).getId() : null;
        log.debug("[Chat] 이전 메시지 조회 완료: chatRoomId={}, userId={}, beforeMessageId={}, size={}, hasNext={}",
            chatRoomId, userId, beforeMessageId, size, hasNext);

        return ChatMessageListResponse.of(assembleResponses(messages), size, hasNext, nextCursor);
    }

    @Transactional
    public ChatMessageDeleteResponse delete(Long messageId, Long userId) {
        ChatMessage message = findMessage(messageId);

        validateOwner(message, userId);
        chatRoomMemberFinder.validateMember(message.getChatRoomId(), userId);

        message.delete();
        chatMessageMapper.delete(message);
        log.info("[Chat] 메시지 삭제 완료: chatRoomId={}, messageId={}, userId={}",
            message.getChatRoomId(), messageId, userId);

        return ChatMessageDeleteResponse.of(message);
    }

    private List<ChatMessageResponse> assembleResponses(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }

        Map<Long, User> sendersById = findSenders(messages);
        Map<Long, List<ChatAttachment>> attachmentsByMessageId = findAttachments(messages);
        Map<Long, SharedCertificationResponse> sharedCertificationsByPostId = findSharedCertifications(messages);

        return messages.stream()
            .map(message -> ChatMessageResponse.of(
                message,
                sendersById.get(message.getSenderId()),
                attachmentsByMessageId.getOrDefault(message.getId(), List.of()),
                sharedCertificationsByPostId.get(message.getReferenceId())
            ))
            .toList();
    }

    private ChatMessage findMessage(Long messageId) {
        return chatMessageMapper.findById(messageId)
            .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
    }

    private Map<Long, User> findSenders(List<ChatMessage> messages) {
        List<Long> senderIds = messages.stream().map(ChatMessage::getSenderId).distinct().toList();

        return userMapper.findByIds(senderIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, List<ChatAttachment>> findAttachments(List<ChatMessage> messages) {
        List<Long> messageIds = messages.stream().map(ChatMessage::getId).toList();

        return chatAttachmentMapper.findByMessageIds(messageIds).stream()
            .collect(Collectors.groupingBy(ChatAttachment::getMessageId));
    }

    private Map<Long, SharedCertificationResponse> findSharedCertifications(List<ChatMessage> messages) {
        List<Long> postIds = messages.stream()
            .filter(message -> message.getReferenceType() == ChatReferenceType.CHALLENGE_CERTIFICATION)
            .map(ChatMessage::getReferenceId)
            .distinct()
            .toList();

        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return certificationMapper.findSharedCertifications(postIds).stream()
            .collect(Collectors.toMap(SharedCertificationInfo::certificationId, SharedCertificationResponse::of));
    }

    private void validateOwner(ChatMessage message, Long userId) {
        if (!message.getSenderId().equals(userId)) {
            throw new ApiException(ErrorCode.CHAT_MESSAGE_NOT_OWNER);
        }
    }

    private void validateMessage(ChatMessageType messageType, String content, ChatReferenceType referenceType,
                                  Long referenceId, MultipartFile file) {
        validateMessageType(messageType);
        validateContent(messageType, content);
        validateFile(messageType, file);
        validateReference(messageType, referenceType, referenceId);
    }

    private void validateMessageType(ChatMessageType messageType) {
        if (messageType != ChatMessageType.TEXT && messageType != ChatMessageType.IMAGE
            && messageType != ChatMessageType.FILE && messageType != ChatMessageType.CERTIFICATION_SHARE) {
            throw new ApiException(ErrorCode.CHAT_INVALID_MESSAGE_TYPE);
        }
    }

    private void validateContent(ChatMessageType messageType, String content) {
        if (messageType == ChatMessageType.CERTIFICATION_SHARE) {
            return; // 서버가 고정 문자열로 대체하므로 클라이언트가 보낸 값은 검증하지 않는다
        }
        if (messageType == ChatMessageType.TEXT && (content == null || content.isBlank())) {
            throw new ApiException(ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED);
        }
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new ApiException(ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG);
        }
    }

    private void validateReference(ChatMessageType messageType, ChatReferenceType referenceType, Long referenceId) {
        if (messageType != ChatMessageType.CERTIFICATION_SHARE) {
            return;
        }
        if (referenceType != ChatReferenceType.CHALLENGE_CERTIFICATION || referenceId == null) {
            throw new ApiException(ErrorCode.CHAT_REFERENCE_REQUIRED);
        }
    }

    private String resolveContent(ChatMessageType messageType, String content) {
        return messageType == ChatMessageType.CERTIFICATION_SHARE ? CERTIFICATION_SHARE_CONTENT : content;
    }

    private SharedCertificationResponse findSharedCertification(Long postId) {
        return certificationMapper.findSharedCertifications(List.of(postId)).stream()
            .findFirst()
            .map(SharedCertificationResponse::of)
            .orElse(null);
    }

    private void validateFile(ChatMessageType messageType, MultipartFile file) {
        boolean hasFile = file != null && !file.isEmpty();
        boolean requiresFile = messageType == ChatMessageType.IMAGE || messageType == ChatMessageType.FILE;

        if (requiresFile && !hasFile) {
            throw new ApiException(ErrorCode.CHAT_ATTACHMENT_REQUIRED);
        }
        if (!requiresFile && hasFile) {
            throw new ApiException(ErrorCode.CHAT_ATTACHMENT_NOT_ALLOWED);
        }
    }

    private ChatFileType toChatFileType(ChatMessageType messageType) {
        return messageType == ChatMessageType.IMAGE ? ChatFileType.IMAGE : ChatFileType.FILE;
    }

    private void updateSenderReadCursor(ChatRoomMember member, Long messageId) {
        member.markRead(messageId);
        chatRoomMemberMapper.updateLastRead(member);
    }
}
