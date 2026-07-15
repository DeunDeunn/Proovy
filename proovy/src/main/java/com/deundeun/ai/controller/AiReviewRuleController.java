package com.deundeun.ai.controller;

import com.deundeun.ai.dto.AiReviewRuleRequest;
import com.deundeun.ai.dto.AiReviewRuleResponse;
import com.deundeun.ai.service.AiReviewRuleService;
import com.deundeun.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges/{challengeId}/ai-review-rule")
public class AiReviewRuleController {

    private final AiReviewRuleService aiReviewRuleService;

    @GetMapping
    public ApiResponse<AiReviewRuleResponse> findAiReviewRule(
            @RequestHeader("X-User-Id") Long id,
            @PathVariable Long challengeId
    ) {
        AiReviewRuleResponse response = aiReviewRuleService.findAiReviewRuleByChallengeId(id, challengeId);
        return ApiResponse.success(response);
    }

    @PutMapping
    public ApiResponse<AiReviewRuleResponse> upsertAiReviewRule(
            @RequestHeader("X-User-Id") Long id,
            @PathVariable Long challengeId,
            @RequestBody AiReviewRuleRequest request
    ) {
        AiReviewRuleResponse response = aiReviewRuleService.upsertAiReviewRule(id, challengeId, request);
        return ApiResponse.success(response);
    }

    @PatchMapping("/review-mode")
    public ApiResponse<AiReviewRuleResponse> updateAiReviewMode(
            @RequestHeader("X-User-Id") Long id,
            @PathVariable Long challengeId,
            @RequestParam String reviewMode
    ) {
        AiReviewRuleResponse response = aiReviewRuleService.updateAiReviewModeByChallengeId(id, challengeId, reviewMode);
        return ApiResponse.success(response);
    }
}
