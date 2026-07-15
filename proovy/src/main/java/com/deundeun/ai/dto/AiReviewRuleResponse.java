package com.deundeun.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AiReviewRuleResponse {

    private Long id;
    private Long challengeId;
    private Long hostId;
    private String ruleText;
    private String reviewMode;
    private boolean active;
    private LocalDateTime updatedAt;
}
