package com.deundeun.ai.controller;

import com.deundeun.ai.dto.AiPageResponse;
import com.deundeun.ai.dto.AiReviewResultItemResponse;
import com.deundeun.ai.service.AiReviewService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges/{challengeId}/ai-review-results")
public class AiReviewResultController {

    private final AiReviewService aiReviewService;

    @GetMapping
    public ApiResponse<AiPageResponse<AiReviewResultItemResponse>> findReviewResults(
            @PathVariable Long challengeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long requesterId = CurrentUser.getUserId();
        return ApiResponse.success(aiReviewService.findReviewResultsByChallengeId(
                requesterId,
                challengeId,
                page,
                size
        ));
    }
}
