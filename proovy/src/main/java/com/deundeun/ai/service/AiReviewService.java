package com.deundeun.ai.service;

import com.deundeun.ai.dto.AiPageResponse;
import com.deundeun.ai.dto.AiReviewResponse;
import com.deundeun.ai.dto.AiReviewResultItemResponse;

public interface AiReviewService {

    AiReviewResponse review(Long requesterId, Long postId);

    AiPageResponse<AiReviewResultItemResponse> findReviewResultsByChallengeId(
            Long requesterId,
            Long challengeId,
            int page,
            int size
    );
}
