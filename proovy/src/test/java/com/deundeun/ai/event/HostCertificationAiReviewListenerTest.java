package com.deundeun.ai.event;

import com.deundeun.ai.service.AiReviewService;
import com.deundeun.notification.event.VerificationSubmittedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@DisplayName("방장 인증글 자동 AI 검수 이벤트 리스너")
@ExtendWith(MockitoExtension.class)
class HostCertificationAiReviewListenerTest {

    @Mock
    private AiReviewService aiReviewService;

    @InjectMocks
    private HostCertificationAiReviewListener listener;

    @Test
    @DisplayName("인증글 등록 이벤트의 게시물 번호로 자동 검수를 요청한다")
    void handle_requestsHostPostReview() {
        listener.handle(new VerificationSubmittedEvent(1L, 10L));

        verify(aiReviewService).reviewSubmittedPost(10L);
    }

    @Test
    @DisplayName("AI 검수 실패가 인증글 등록 이벤트 처리로 전파되지 않는다")
    void handle_reviewFailure_isContained() {
        doThrow(new IllegalStateException("AI failure")).when(aiReviewService).reviewSubmittedPost(10L);

        assertThatCode(() -> listener.handle(new VerificationSubmittedEvent(1L, 10L)))
                .doesNotThrowAnyException();
    }
}
