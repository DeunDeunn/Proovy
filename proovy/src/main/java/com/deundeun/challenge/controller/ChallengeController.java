package com.deundeun.challenge.controller;

import com.deundeun.challenge.dto.request.ChallengeCreateRequest;
import com.deundeun.challenge.dto.request.ChallengeSearchCondition;
import com.deundeun.challenge.dto.response.ChallengeCreateResponse;
import com.deundeun.challenge.dto.response.ChallengeDetailResponse;
import com.deundeun.challenge.dto.response.ChallengeSummaryResponse;
import com.deundeun.challenge.dto.response.PageResponse;
import com.deundeun.challenge.service.ChallengeService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping
    public ApiResponse<ChallengeCreateResponse> createChallenge(
            @Valid @RequestBody ChallengeCreateRequest request) {
        Long hostId = CurrentUser.getUserId();
        return ApiResponse.success(challengeService.create(hostId, request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ChallengeSummaryResponse>> searchChallenges(
            @ModelAttribute ChallengeSearchCondition condition) {
        return ApiResponse.success(challengeService.search(condition));
    }

    @GetMapping("/{challengeId}")
    public ApiResponse<ChallengeDetailResponse> getChallengeDetail(
            @PathVariable Long challengeId) {
        return ApiResponse.success(challengeService.getDetail(challengeId));
    }


}
