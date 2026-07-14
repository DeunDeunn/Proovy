package com.deundeun.chat.controller;

import com.deundeun.chat.dto.request.DirectChatRoomRequest;
import com.deundeun.chat.dto.response.ChallengeChatRoomResponse;
import com.deundeun.chat.dto.response.ChatRoomReadResponse;
import com.deundeun.chat.dto.response.DirectChatRoomResponse;
import com.deundeun.chat.service.ChatRoomMemberService;
import com.deundeun.chat.service.ChatRoomService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;

    @PostMapping("/direct-rooms")
    public ApiResponse<DirectChatRoomResponse> createOrGetDirectRoom(
        @RequestParam Long userId, //TODO 인증 붙으면 로그인 사용자 ID로 대체
        @RequestBody @Valid DirectChatRoomRequest request
    ) {
        DirectChatRoomResponse response = chatRoomService.createOrGetDirectRoom(userId, request.targetUserId());
        String message = response.created() ? "1:1 채팅방을 생성했습니다." : "1:1 채팅방을 조회했습니다.";

        return ApiResponse.success(response, message);
    }

    @GetMapping("/challenge-rooms/{challengeId}")
    public ApiResponse<ChallengeChatRoomResponse> getChallengeRoom(
        @RequestParam Long userId, //TODO 인증 붙으면 로그인 사용자 ID로 대체
        @PathVariable Long challengeId
    ) {
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
