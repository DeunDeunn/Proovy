package com.deundeun.ai.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReviewResultVo {

    private Long id;
    private Long challengeId;
    private Long hostId;
    private Long verificationPostId;
    private String reviewMode;
    private String decision;
    private BigDecimal confidence;
    private String reason;
    private String rawResponse;
    private String status;
    private String previousPostStatus;
    private String newPostStatus;
    private LocalDateTime createdAt;
}
