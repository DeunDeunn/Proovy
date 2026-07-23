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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        verify(aiReviewService, never()).rejectHostPostAfterReviewFailure(10L);
    }

    @Test
    @DisplayName("AI 검수가 세 번 실패하면 방장 글 자동 반려를 요청한다")
    void handle_reviewFailure_retriesAndRejectsHostPost() {
        doThrow(new IllegalStateException("AI failure")).when(aiReviewService).reviewSubmittedPost(10L);

        assertThatCode(() -> listener.handle(new VerificationSubmittedEvent(1L, 10L)))
                .doesNotThrowAnyException();

        verify(aiReviewService, times(3)).reviewSubmittedPost(10L);
        verify(aiReviewService).rejectHostPostAfterReviewFailure(10L);
    }

    @Test
    @DisplayName("재시도에서 성공하면 자동 반려하지 않는다")
    void handle_retrySucceeds_doesNotRejectHostPost() {
        doThrow(new IllegalStateException("temporary AI failure"))
                .doNothing()
                .when(aiReviewService).reviewSubmittedPost(10L);

        listener.handle(new VerificationSubmittedEvent(1L, 10L));

        verify(aiReviewService, times(2)).reviewSubmittedPost(10L);
        verify(aiReviewService, never()).rejectHostPostAfterReviewFailure(10L);
    }
}
