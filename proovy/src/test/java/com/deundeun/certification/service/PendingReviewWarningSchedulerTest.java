package com.deundeun.certification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PendingReviewWarningScheduler - 자정 트리거")
class PendingReviewWarningSchedulerTest {

    @Mock
    private PendingReviewWarningService pendingReviewWarningService;

    @InjectMocks
    private PendingReviewWarningScheduler pendingReviewWarningScheduler;

    @Test
    @DisplayName("미검수 경고 서비스에 위임한다")
    void delegatesToService() {
        pendingReviewWarningScheduler.runMidnightPendingReviewWarning();

        verify(pendingReviewWarningService).warnForPendingReviews();
    }

    @Test
    @DisplayName("서비스 예외를 스케줄러 밖으로 전파하지 않는다")
    void swallowsException() {
        doThrow(new RuntimeException("boom"))
                .when(pendingReviewWarningService).warnForPendingReviews();

        assertThatCode(() -> pendingReviewWarningScheduler.runMidnightPendingReviewWarning())
                .doesNotThrowAnyException();
    }
}
