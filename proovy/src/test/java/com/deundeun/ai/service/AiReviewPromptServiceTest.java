package com.deundeun.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.deundeun.ai.dto.AiReviewContext;
import com.deundeun.ai.vo.AiReviewRuleVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

@DisplayName("AiReviewPromptService")
class AiReviewPromptServiceTest {

    private final AiReviewPromptService aiReviewPromptService = new AiReviewPromptService();

    @Test
    @DisplayName("검수 기준, 인증글 내용, 대표 이미지를 포함한 프롬프트를 생성한다")
    void createPrompt_containsRulePostImageAndDecisionSchema() {
        AiReviewContext context = context();
        AiReviewRuleVo rule = AiReviewRuleVo.builder()
                .ruleText("운동복을 입고 실제 운동 중인 사진이어야 한다.")
                .build();
        List<String> imageUrls = List.of(
                "https://s3.example.com/posts/thumb.jpg",
                "https://s3.example.com/posts/detail.jpg"
        );

        String prompt = aiReviewPromptService.createPrompt(context, rule, imageUrls);

        assertThat(prompt)
                .contains("너는 챌린지 인증글을 검수하는 AI다.")
                .contains("아침 운동 챌린지")
                .contains("운동 사진 업로드")
                .contains("운동복을 입고 실제 운동 중인 사진이어야 한다.")
                .contains("헬스장에서 러닝머신 30분 완료")
                .contains("https://s3.example.com/posts/thumb.jpg")
                .contains("https://s3.example.com/posts/detail.jpg")
                .contains("\"decision\": \"APPROVED\" | \"REJECTED\" | \"NEEDS_REVIEW\"")
                .contains("\"reason\": \"판단 이유\"")
                .contains("\"confidence\": 0.0");
    }

    private AiReviewContext context() {
        AiReviewContext context = new AiReviewContext();
        ReflectionTestUtils.setField(context, "challengeTitle", "아침 운동 챌린지");
        ReflectionTestUtils.setField(context, "verificationMethod", "운동 사진 업로드");
        ReflectionTestUtils.setField(context, "postContent", "헬스장에서 러닝머신 30분 완료");
        return context;
    }
}
