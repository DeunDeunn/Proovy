package com.deundeun.certification.service;

import com.deundeun.certification.mapper.HostWarningMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PendingReviewWarningService - 자정 미검수 경고 배치")
class PendingReviewWarningServiceTest {

    @Mock
    private HostWarningMapper hostWarningMapper;

    @Mock
    private PendingReviewWarningProcessor pendingReviewWarningProcessor;

    @InjectMocks
    private PendingReviewWarningService pendingReviewWarningService;

    @Test
    @DisplayName("PENDING 참가자 글이 없으면 후속 처리를 하지 않는다")
    void noPendingReview_doesNothing() {
        when(hostWarningMapper.findPendingReviewHostIds()).thenReturn(List.of());

        pendingReviewWarningService.warnForPendingReviews();

        verify(hostWarningMapper).findPendingReviewHostIds();
        verifyNoMoreInteractions(hostWarningMapper);
        verifyNoMoreInteractions(pendingReviewWarningProcessor);
    }

    @Test
    @DisplayName("미검수 챌린지의 방장을 각각 독립 처리한다")
    void pendingReviews_processEachHost() {
        when(hostWarningMapper.findPendingReviewHostIds()).thenReturn(List.of(100L, 200L));
        when(pendingReviewWarningProcessor.processHost(100L)).thenReturn(1);
        when(pendingReviewWarningProcessor.processHost(200L)).thenReturn(2);

        pendingReviewWarningService.warnForPendingReviews();

        verify(pendingReviewWarningProcessor).processHost(100L);
        verify(pendingReviewWarningProcessor).processHost(200L);
    }

    @Test
    @DisplayName("한 방장 처리에 실패해도 다음 방장 처리를 계속한다")
    void oneHostFailure_continuesWithNextHost() {
        when(hostWarningMapper.findPendingReviewHostIds()).thenReturn(List.of(100L, 200L));
        when(pendingReviewWarningProcessor.processHost(100L))
                .thenThrow(new IllegalStateException("처리 실패"));
        when(pendingReviewWarningProcessor.processHost(200L)).thenReturn(1);

        assertThatCode(() -> pendingReviewWarningService.warnForPendingReviews())
                .doesNotThrowAnyException();

        verify(pendingReviewWarningProcessor).processHost(100L);
        verify(pendingReviewWarningProcessor).processHost(200L);
    }
}
