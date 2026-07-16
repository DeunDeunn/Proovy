package com.deundeun.ai.dto;

import com.deundeun.ai.vo.AiReviewResultVo;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AiReviewResponse {

    private Long id;
    private Long challengeId;
    private Long hostId;
    private Long reviewImageId;
    private Long verificationPostId;
    private String reviewMode;
    private String decision;
    private BigDecimal confidence;
    private String reason;
    private String status;
    private String previousPostStatus;
    private String newPostStatus;
    private LocalDateTime createdAt;

    public static AiReviewResponse from(AiReviewResultVo result) {
        return AiReviewResponse.builder()
                .id(result.getId())
                .challengeId(result.getChallengeId())
                .hostId(result.getHostId())
                .reviewImageId(result.getReviewImageId())
                .verificationPostId(result.getVerificationPostId())
                .reviewMode(result.getReviewMode())
                .decision(result.getDecision())
                .confidence(result.getConfidence())
                .reason(result.getReason())
                .status(result.getStatus())
                .previousPostStatus(result.getPreviousPostStatus())
                .newPostStatus(result.getNewPostStatus())
                .createdAt(result.getCreatedAt())
                .build();
    }
}
