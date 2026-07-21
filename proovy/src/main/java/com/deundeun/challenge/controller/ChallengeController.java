package com.deundeun.challenge.controller;

import com.deundeun.challenge.dto.request.ChallengeCreateRequest;
import com.deundeun.challenge.dto.request.ChallengeSearchCondition;
import com.deundeun.challenge.dto.request.ChallengeUpdateRequest;
import com.deundeun.challenge.dto.response.ChallengeCreateResponse;
import com.deundeun.challenge.dto.response.ChallengeDetailResponse;
import com.deundeun.challenge.dto.response.ChallengeProgressResponse;
import com.deundeun.challenge.dto.response.ChallengeSummaryResponse;
import com.deundeun.challenge.dto.response.ChallengeThumbnailUpdateResponse;
import com.deundeun.challenge.dto.response.PageResponse;
import com.deundeun.challenge.service.ChallengeService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @GetMapping("/me")
    public ApiResponse<List<ChallengeSummaryResponse>> getMyChallenges() {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(challengeService.getMyChallenges(userId));
    }

    @GetMapping("/{challengeId}")
    public ApiResponse<ChallengeDetailResponse> getChallengeDetail(
            @PathVariable Long challengeId) {
        return ApiResponse.success(challengeService.getDetail(challengeId));
    }

    @GetMapping("/{challengeId}/summary")
    public ApiResponse<ChallengeProgressResponse> getChallengeProgress(
            @PathVariable Long challengeId) {
        return ApiResponse.success(challengeService.getProgress(challengeId));
    }

    @PatchMapping("/{challengeId}")
    public ApiResponse<Void> updateChallenge(
            @PathVariable Long challengeId,
            @Valid @RequestBody ChallengeUpdateRequest request) {
        Long userId = CurrentUser.getUserId();
        challengeService.update(challengeId, userId, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{challengeId}")
    public ApiResponse<Void> cancelChallenge(@PathVariable Long challengeId) {
        Long userId = CurrentUser.getUserId();
        challengeService.cancel(challengeId, userId);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{challengeId}/thumbnail")
    public ApiResponse<ChallengeThumbnailUpdateResponse> updateThumbnail(
            @PathVariable Long challengeId,
            @RequestParam("image") MultipartFile image) {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(challengeService.updateThumbnail(challengeId, userId, image));
    }

}
