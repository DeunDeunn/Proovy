package com.deundeun.ai.dto;

import com.deundeun.ai.vo.AiReviewResultVo;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AiReviewResultItemResponse {

    private Long id;
    private Long verificationPostId;
    private String reviewMode;
    private String decision;
    private BigDecimal confidence;
    private String reason;
    private String aiReviewStatus;
    private String newPostStatus;
    private LocalDateTime createdAt;

    public static AiReviewResultItemResponse from(AiReviewResultVo result) {
        return AiReviewResultItemResponse.builder()
                .id(result.getId())
                .verificationPostId(result.getVerificationPostId())
                .reviewMode(toResponseReviewMode(result.getReviewMode()))
                .decision(result.getDecision())
                .confidence(result.getConfidence())
                .reason(result.getReason())
                .aiReviewStatus(toResponseStatus(result.getStatus()))
                .newPostStatus(result.getNewPostStatus())
                .createdAt(result.getCreatedAt())
                .build();
    }

    private static String toResponseReviewMode(String reviewMode) {
        if ("AUTO".equals(reviewMode)) {
            return "AUTO_DECISION";
        }
        if ("MANUAL".equals(reviewMode)) {
            return "ASSIST_ONLY";
        }
        return reviewMode;
    }

    private static String toResponseStatus(String status) {
        if ("COMPLETED".equals(status)) {
            return "SUCCESS";
        }
        return status;
    }
}
