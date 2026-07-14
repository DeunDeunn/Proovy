package com.deundeun.chat.controller;

import com.deundeun.chat.dto.response.ChatMessageListResponse;
import com.deundeun.chat.service.ChatMessageService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

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
}
