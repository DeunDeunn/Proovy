package com.deundeun.ai.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReviewRuleRequest {

    private String reviewMode;
    private String ruleText;
}
