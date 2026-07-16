package com.deundeun.ai.client;

import com.deundeun.ai.dto.AiReviewAiResult;

import java.util.List;

public interface AiReviewClient {

    AiReviewAiResult review(String prompt, List<String> imageUrls);
}
