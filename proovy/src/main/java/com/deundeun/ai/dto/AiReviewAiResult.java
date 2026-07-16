package com.deundeun.ai.dto;

import com.deundeun.ai.enums.AiReviewDecision;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiReviewAiResult {

    private AiReviewDecision decision;
    private String reason;
    private double confidence;
    private String rawResponse;
}
