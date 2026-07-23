package com.deundeun.certification.service;

import com.deundeun.certification.mapper.HostWarningMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PendingReviewWarningProcessor - 방장별 미검수 경고 처리")
class PendingReviewWarningProcessorTest {

    @Mock
    private HostWarningMapper hostWarningMapper;

    @InjectMocks
    private PendingReviewWarningProcessor pendingReviewWarningProcessor;

    @Test
    @DisplayName("방장 처리는 독립된 새 트랜잭션에서 실행한다")
    void processHost_usesRequiresNewTransaction() throws NoSuchMethodException {
        Transactional transactional = PendingReviewWarningProcessor.class
                .getMethod("processHost", Long.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    @DisplayName("처리 시점에 미검수 글이 없으면 페널티를 확인하지 않는다")
    void noPendingReview_doesNothing() {
        when(hostWarningMapper.insertWarningsForPendingChallenges(100L)).thenReturn(List.of());

        int warningCount = pendingReviewWarningProcessor.processHost(100L);

        assertThat(warningCount).isZero();
        verify(hostWarningMapper, never()).countActiveWarnings(anyLong());
    }

    @Test
    @DisplayName("경고 3회 미만이면 강등과 개설 제한을 적용하지 않는다")
    void underLimit_noPenalty() {
        when(hostWarningMapper.insertWarningsForPendingChallenges(100L)).thenReturn(List.of(100L));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(2);

        int warningCount = pendingReviewWarningProcessor.processHost(100L);

        assertThat(warningCount).isEqualTo(1);
        verify(hostWarningMapper, never()).revokeVerification(anyLong());
        verify(hostWarningMapper, never()).setPenaltyDate(anyLong(), any());
        verify(hostWarningMapper, never()).resolveActiveWarnings(anyLong());
    }

    @Test
    @DisplayName("경고 3회에 도달한 우수회원은 강등하고 경고를 소진한다")
    void limitReached_excellentMember_demoted() {
        when(hostWarningMapper.insertWarningsForPendingChallenges(100L)).thenReturn(List.of(100L));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(3);
        when(hostWarningMapper.isExcellentMember(100L)).thenReturn(true);

        pendingReviewWarningProcessor.processHost(100L);

        verify(hostWarningMapper).revokeVerification(100L);
        verify(hostWarningMapper).setDemotedAt(100L);
        verify(hostWarningMapper, never()).setPenaltyDate(anyLong(), any());
        verify(hostWarningMapper).resolveActiveWarnings(100L);
    }

    @Test
    @DisplayName("경고 3회에 도달한 일반회원은 개설 제한을 적용하고 경고를 소진한다")
    void limitReached_normalMember_penalized() {
        when(hostWarningMapper.insertWarningsForPendingChallenges(100L)).thenReturn(List.of(100L));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(3);
        when(hostWarningMapper.isExcellentMember(100L)).thenReturn(false);

        pendingReviewWarningProcessor.processHost(100L);

        verify(hostWarningMapper).setPenaltyDate(eq(100L), any());
        verify(hostWarningMapper, never()).revokeVerification(anyLong());
        verify(hostWarningMapper, never()).setDemotedAt(anyLong());
        verify(hostWarningMapper).resolveActiveWarnings(100L);
    }
}
