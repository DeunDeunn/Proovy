package com.deundeun.chat.controller;

import com.deundeun.chat.domain.ChatMessageType;
import com.deundeun.chat.dto.response.ChatMessageListResponse;
import com.deundeun.chat.dto.response.ChatMessageResponse;
import com.deundeun.chat.service.ChatMessageService;
import com.deundeun.chat.service.support.ChatMessageBroadcaster;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
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
}
