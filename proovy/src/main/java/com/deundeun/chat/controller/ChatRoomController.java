package com.deundeun.chat.controller;

import com.deundeun.chat.dto.request.DirectChatRoomRequest;
import com.deundeun.chat.dto.response.ChallengeChatRoomResponse;
import com.deundeun.chat.dto.response.ChatRoomListResponse;
import com.deundeun.chat.dto.response.ChatRoomReadResponse;
import com.deundeun.chat.dto.response.DirectChatRoomResponse;
import com.deundeun.chat.service.ChatRoomMemberService;
import com.deundeun.chat.service.ChatRoomService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;

    @PostMapping("/direct-rooms")
    public ApiResponse<DirectChatRoomResponse> createOrGetDirectRoom(
        @RequestBody @Valid DirectChatRoomRequest request
    ) {
        Long userId = CurrentUser.getUserId();
        DirectChatRoomResponse response = chatRoomService.createOrGetDirectRoom(userId, request.targetUserId());
        String message = response.created() ? "1:1 채팅방을 생성했습니다." : "1:1 채팅방을 조회했습니다.";

        return ApiResponse.success(response, message);
    }

    @GetMapping("/rooms")
    public ApiResponse<ChatRoomListResponse> getMyRooms(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Long userId = CurrentUser.getUserId();
        ChatRoomListResponse response = chatRoomService.getMyRooms(userId, page, size);

        return ApiResponse.success(response);
    }

    @GetMapping("/challenge-rooms/{challengeId}")
    public ApiResponse<ChallengeChatRoomResponse> getChallengeRoom(@PathVariable Long challengeId) {
        Long userId = CurrentUser.getUserId();
        ChallengeChatRoomResponse response = chatRoomService.getChallengeRoom(challengeId, userId);

        return ApiResponse.success(response, "챌린지 채팅방을 조회했습니다.");
    }

    @PatchMapping("/rooms/{chatRoomId}/read")
    public ApiResponse<ChatRoomReadResponse> markAsRead(@PathVariable Long chatRoomId) {
        Long userId = CurrentUser.getUserId();
        ChatRoomReadResponse response = chatRoomMemberService.markAsRead(chatRoomId, userId);

        return ApiResponse.success(response);
    }
}
