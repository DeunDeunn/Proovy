package com.deundeun.chat.controller;

import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.domain.ChatReferenceType;
import com.deundeun.chat.dto.request.CertificationShareRequest;
import com.deundeun.chat.dto.request.ChatMessageSendRequest;
import com.deundeun.chat.dto.response.ChatMessageDeleteResponse;
import com.deundeun.chat.dto.response.ChatMessageListResponse;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import com.deundeun.chat.service.ChatMessageService;
import com.deundeun.chat.service.support.ChatMessageBroadcaster;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ChatMessageBroadcaster chatMessageBroadcaster;

    @GetMapping("/rooms/{chatRoomId}/messages")
    public ApiResponse<ChatMessageListResponse> getMessages(
        @PathVariable Long chatRoomId,
        @RequestParam(required = false) Long beforeMessageId,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size
    ) {
        Long userId = CurrentUser.getUserId();
        ChatMessageListResponse response = chatMessageService.getMessages(chatRoomId, userId, beforeMessageId, size);

        return ApiResponse.success(response);
    }

    @PostMapping(value = "/rooms/{chatRoomId}/attachments/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ChatMessageResponse> sendWithAttachment(
        @PathVariable Long chatRoomId,
        @RequestParam ChatMessageType messageType,
        @RequestParam(required = false) String content,
        @RequestPart(required = false) MultipartFile file
    ) {
        Long senderId = CurrentUser.getUserId();
        ChatMessageResponse response = chatMessageService.sendAttachment(chatRoomId, senderId, messageType, content, file);
        chatMessageBroadcaster.broadcast(chatRoomId, response);

        return ApiResponse.success(response);
    }

    // WS 연결 없이도(예: 인증글 상세 페이지) 공유할 수 있도록 REST로 제공한다.
    @PostMapping("/rooms/{chatRoomId}/certification-shares")
    public ApiResponse<ChatMessageResponse> shareCertification(
        @PathVariable Long chatRoomId,
        @RequestBody @Valid CertificationShareRequest request
    ) {
        Long senderId = CurrentUser.getUserId();
        ChatMessageSendRequest sendRequest = new ChatMessageSendRequest(
            ChatMessageType.CERTIFICATION_SHARE, null, ChatReferenceType.CHALLENGE_CERTIFICATION, request.certificationId());
        ChatMessageResponse response = chatMessageService.send(chatRoomId, senderId, sendRequest, null);
        chatMessageBroadcaster.broadcast(chatRoomId, response);

        return ApiResponse.success(response);
    }

    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<ChatMessageDeleteResponse> deleteMessage(@PathVariable Long messageId) {
        Long userId = CurrentUser.getUserId();
        ChatMessageDeleteResponse response = chatMessageService.delete(messageId, userId);
        chatMessageBroadcaster.broadcast(response);

        return ApiResponse.success(response);
    }
}
