package com.deundeun.ai.service;

import com.deundeun.ai.dto.AiReviewContext;
import com.deundeun.ai.vo.AiReviewRuleVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiReviewPromptService {

    private static final String ROLE_PROMPT = "너는 챌린지 인증글을 검수하는 AI다.";

    private static final String RESPONSE_FORMAT = """
            반드시 아래 JSON 형식으로만 응답해라.
            {
              "decision": "APPROVED" | "REJECTED" | "NEEDS_REVIEW",
              "reason": "판단 이유",
              "confidence": 0.0
            }
            """;

    private static final String REVIEW_PROMPT_TEMPLATE = """
            %s

            [챌린지 정보]
            제목: %s
            인증 방식: %s

            [방장이 등록한 검수 기준]
            %s

            [참가자가 제출한 인증 내용]
            %s

            [이미지 URL]
            %s

            %s
            """;

    public String createPrompt(AiReviewContext context, AiReviewRuleVo rule, List<String> imageUrls) {
        return REVIEW_PROMPT_TEMPLATE.formatted(
                ROLE_PROMPT,
                valueOrEmpty(context.getChallengeTitle()),
                valueOrEmpty(context.getVerificationMethod()),
                valueOrEmpty(rule.getRuleText()),
                valueOrEmpty(context.getPostContent()),
                imageUrls,
                RESPONSE_FORMAT
        );
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
