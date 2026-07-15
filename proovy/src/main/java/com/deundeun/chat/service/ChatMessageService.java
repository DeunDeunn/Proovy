package com.deundeun.chat.service;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.certification.dto.chat.SharedCertificationInfo;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.chat.domain.ChatAttachment;
import com.deundeun.chat.domain.ChatMessage;
import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatReferenceType;
import com.deundeun.chat.domain.ChatRoomMember;
import com.deundeun.chat.dto.request.ChatMessageSendRequest;
import com.deundeun.chat.dto.response.ChatMessageListResponse;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import com.deundeun.chat.dto.response.SharedCertificationResponse;
import com.deundeun.chat.mapper.ChatAttachmentMapper;
import com.deundeun.chat.mapper.ChatMessageMapper;
import com.deundeun.chat.mapper.ChatRoomMemberMapper;
import com.deundeun.chat.service.support.ChatRoomMemberFinder;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final ChatMessageMapper chatMessageMapper;
    private final ChatAttachmentMapper chatAttachmentMapper;
    private final ChatRoomMemberMapper chatRoomMemberMapper;
    private final UserMapper userMapper;
    private final CertificationMapper certificationMapper;
    private final ChatRoomMemberFinder chatRoomMemberFinder;

    @Transactional
    public ChatMessageResponse send(Long chatRoomId, Long senderId, ChatMessageSendRequest request) {
        ChatRoomMember member = chatRoomMemberFinder.findMember(chatRoomId, senderId);
        ChatMessageType messageType = request.messageType();

        validateMessageType(messageType);
        validateContent(messageType, request.content());
        validateAttachments(request.attachmentIds());

        ChatMessage message = ChatMessage.create(chatRoomId, senderId, request.content(), messageType, null, null);
        chatMessageMapper.insert(message);

        updateSenderReadCursor(member, message.getId());

        User sender = userMapper.findById(senderId);
        log.info("[Chat] 메시지 전송 완료: chatRoomId={}, senderId={}, messageId={}, messageType={}",
            chatRoomId, senderId, message.getId(), messageType);

        return ChatMessageResponse.of(message, sender, List.of(), null);
    }

    private void validateMessageType(ChatMessageType messageType) {
        if (messageType != ChatMessageType.TEXT) {
            throw new ApiException(ErrorCode.CHAT_INVALID_MESSAGE_TYPE);
        }
    }

    private void validateContent(ChatMessageType messageType, String content) {
        if (messageType == ChatMessageType.TEXT && (content == null || content.isBlank())) {
            throw new ApiException(ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED);
        }
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new ApiException(ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG);
        }
    }

    private void validateAttachments(List<Long> attachmentIds) {
        if (attachmentIds != null) {
            throw new ApiException(ErrorCode.CHAT_ATTACHMENT_NOT_ALLOWED);
        }
    }

    private void updateSenderReadCursor(ChatRoomMember member, Long messageId) {
        member.markRead(messageId);
        chatRoomMemberMapper.updateLastRead(member);
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
}
