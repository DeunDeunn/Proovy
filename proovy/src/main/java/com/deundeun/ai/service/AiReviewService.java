package com.deundeun.ai.service;

import com.deundeun.ai.dto.AiReviewResponse;

public interface AiReviewService {

    AiReviewResponse review(Long requesterId, Long postId);
}
