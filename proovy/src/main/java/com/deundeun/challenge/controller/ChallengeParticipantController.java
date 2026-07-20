package com.deundeun.challenge.controller;

import com.deundeun.challenge.dto.response.ChallengeParticipantListItemResponse;
import com.deundeun.challenge.dto.response.ChallengeParticipantResponse;
import com.deundeun.challenge.service.ChallengeParticipantService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challenges/{challengeId}/participants")
@RequiredArgsConstructor
public class ChallengeParticipantController {

    private final ChallengeParticipantService challengeParticipantService;

    @PostMapping
    public ApiResponse<ChallengeParticipantResponse> join(@PathVariable Long challengeId) {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(challengeParticipantService.join(challengeId, userId));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> leave(@PathVariable Long challengeId) {
        Long userId = CurrentUser.getUserId();
        challengeParticipantService.leave(challengeId, userId);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<ChallengeParticipantListItemResponse>> getParticipants(
            @PathVariable Long challengeId) {
        return ApiResponse.success(challengeParticipantService.getParticipants(challengeId));
    }
}
