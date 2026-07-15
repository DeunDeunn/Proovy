package com.deundeun.ai.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReviewRuleVo {

    private Long id;
    private long hostId;
    private Long challengeId;
    private String ruleText;
    private String reviewMode;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

