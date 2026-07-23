package com.deundeun.certification.service;

import com.deundeun.certification.mapper.HostWarningMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PendingReviewWarningService - 자정 미검수 경고 배치")
class PendingReviewWarningServiceTest {

    @Mock
    private HostWarningMapper hostWarningMapper;

    @InjectMocks
    private PendingReviewWarningService pendingReviewWarningService;

    @Test
    @DisplayName("PENDING 참가자 글이 없으면 후속 처리를 하지 않는다")
    void noPendingReview_doesNothing() {
        when(hostWarningMapper.insertWarningsForPendingChallenges()).thenReturn(List.of());

        pendingReviewWarningService.warnForPendingReviews();

        verify(hostWarningMapper).insertWarningsForPendingChallenges();
        verifyNoMoreInteractions(hostWarningMapper);
    }

    @Test
    @DisplayName("미검수 챌린지의 방장에게 경고를 적립하고 게시물 승인 처리는 하지 않는다")
    void pendingReviews_warnHostsWithoutApproval() {
        when(hostWarningMapper.insertWarningsForPendingChallenges()).thenReturn(List.of(100L, 200L));
        when(hostWarningMapper.countActiveWarnings(anyLong())).thenReturn(1);

        pendingReviewWarningService.warnForPendingReviews();

        verify(hostWarningMapper).countActiveWarnings(100L);
        verify(hostWarningMapper).countActiveWarnings(200L);
        verify(hostWarningMapper, never()).revokeVerification(anyLong());
        verify(hostWarningMapper, never()).setPenaltyDate(anyLong(), any());
    }

    @Test
    @DisplayName("한 방장이 여러 챌린지를 방치해도 페널티 판정은 한 번만 한다")
    void sameHost_checksPenaltyOnce() {
        when(hostWarningMapper.insertWarningsForPendingChallenges()).thenReturn(List.of(100L, 100L));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(1);

        pendingReviewWarningService.warnForPendingReviews();

        verify(hostWarningMapper, times(1)).countActiveWarnings(100L);
    }

    @Test
    @DisplayName("경고 3회 미만이면 강등과 개설 제한을 적용하지 않는다")
    void underLimit_noPenalty() {
        when(hostWarningMapper.insertWarningsForPendingChallenges()).thenReturn(List.of(100L));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(2);

        pendingReviewWarningService.warnForPendingReviews();

        verify(hostWarningMapper, never()).revokeVerification(anyLong());
        verify(hostWarningMapper, never()).setPenaltyDate(anyLong(), any());
        verify(hostWarningMapper, never()).resolveActiveWarnings(anyLong());
    }

    @Test
    @DisplayName("경고 3회에 도달한 우수회원은 강등하고 경고를 소진한다")
    void limitReached_excellentMember_demoted() {
        when(hostWarningMapper.insertWarningsForPendingChallenges()).thenReturn(List.of(100L));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(3);
        when(hostWarningMapper.isExcellentMember(100L)).thenReturn(true);

        pendingReviewWarningService.warnForPendingReviews();

        verify(hostWarningMapper).revokeVerification(100L);
        verify(hostWarningMapper).setDemotedAt(100L);
        verify(hostWarningMapper, never()).setPenaltyDate(anyLong(), any());
        verify(hostWarningMapper).resolveActiveWarnings(100L);
    }

    @Test
    @DisplayName("경고 3회에 도달한 일반회원은 개설 제한을 적용하고 경고를 소진한다")
    void limitReached_normalMember_penalized() {
        when(hostWarningMapper.insertWarningsForPendingChallenges()).thenReturn(List.of(100L));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(3);
        when(hostWarningMapper.isExcellentMember(100L)).thenReturn(false);

        pendingReviewWarningService.warnForPendingReviews();

        verify(hostWarningMapper).setPenaltyDate(eq(100L), any());
        verify(hostWarningMapper, never()).revokeVerification(anyLong());
        verify(hostWarningMapper, never()).setDemotedAt(anyLong());
        verify(hostWarningMapper).resolveActiveWarnings(100L);
    }
}
