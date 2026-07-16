package com.deundeun.ai.controller;

import com.deundeun.ai.dto.AiReviewResponse;
import com.deundeun.ai.service.AiReviewService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/certification-posts/{postId}/ai-review")
public class AiReviewController {

    private final AiReviewService aiReviewService;

    @PostMapping
    public ApiResponse<AiReviewResponse> review(@PathVariable Long postId) {
        Long requesterId = CurrentUser.getUserId();
        return ApiResponse.success(aiReviewService.review(requesterId, postId));
    }
}
