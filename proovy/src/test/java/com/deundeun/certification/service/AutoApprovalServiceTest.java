package com.deundeun.certification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.certification.dto.PendingPostForAutoApproval;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.certification.mapper.HostWarningMapper;
import com.deundeun.notification.event.VerificationApprovedEvent;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoApprovalService - 자정 자동승인 배치")
class AutoApprovalServiceTest {

    @Mock
    private CertificationMapper certificationMapper;
    @Mock
    private HostWarningMapper hostWarningMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AutoApprovalService autoApprovalService;

    private PendingPostForAutoApproval post(long postId, long authorId, long challengeId, long hostId) {
        PendingPostForAutoApproval p = new PendingPostForAutoApproval();
        p.setPostId(postId);
        p.setAuthorId(authorId);
        p.setChallengeId(challengeId);
        p.setHostId(hostId);
        return p;
    }

    @Test
    @DisplayName("[A-01] PENDING 글이 없으면 승인·경고를 전혀 하지 않는다")
    void noPending_doesNothing() {
        when(certificationMapper.findAllPendingPostsForAutoApproval()).thenReturn(List.of());

        autoApprovalService.autoApproveAllPending();

        verify(certificationMapper, never()).approvePostsAuto(any());
        verifyNoInteractions(hostWarningMapper);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("[A-02] 일괄 승인 + 방장 경고 + 작성자별 승인 이벤트를 발행한다")
    void approvesAndWarnsAndPublishes() {
        // 챌린지10(방장100) 글1, 챌린지20(방장200) 글2
        when(certificationMapper.findAllPendingPostsForAutoApproval())
                .thenReturn(List.of(post(1, 11, 10, 100), post(2, 22, 20, 200)));
        when(hostWarningMapper.countActiveWarnings(anyLong())).thenReturn(1); // 3회 미만

        autoApprovalService.autoApproveAllPending();

        verify(certificationMapper).approvePostsAuto(List.of(1L, 2L));
        verify(hostWarningMapper).insertWarning(100L, 10L);
        verify(hostWarningMapper).insertWarning(200L, 20L);
        verify(eventPublisher, times(2)).publishEvent(any(VerificationApprovedEvent.class));
    }

    @Test
    @DisplayName("[A-03] 같은 챌린지에 PENDING 글이 여러 개여도 경고는 1건만 쌓는다")
    void sameChallenge_singleWarning() {
        when(certificationMapper.findAllPendingPostsForAutoApproval())
                .thenReturn(List.of(post(1, 11, 10, 100), post(2, 12, 10, 100)));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(1);

        autoApprovalService.autoApproveAllPending();

        verify(hostWarningMapper, times(1)).insertWarning(100L, 10L);
        // 승인 이벤트는 글 단위라 2건
        verify(eventPublisher, times(2)).publishEvent(any(VerificationApprovedEvent.class));
    }

    @Test
    @DisplayName("[A-04] 경고 3회 미만이면 강등·페널티 모두 없다")
    void underLimit_noPenalty() {
        when(certificationMapper.findAllPendingPostsForAutoApproval())
                .thenReturn(List.of(post(1, 11, 10, 100)));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(2);

        autoApprovalService.autoApproveAllPending();

        verify(hostWarningMapper, never()).revokeVerification(anyLong());
        verify(hostWarningMapper, never()).setPenaltyDate(anyLong(), any());
        verify(hostWarningMapper, never()).resolveActiveWarnings(anyLong());
    }

    @Test
    @DisplayName("[A-05] 경고 3회 + 우수회원이면 강등(revoke+demote)하고 경고를 소진한다")
    void limitReached_excellent_demoted() {
        when(certificationMapper.findAllPendingPostsForAutoApproval())
                .thenReturn(List.of(post(1, 11, 10, 100)));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(3);
        when(hostWarningMapper.isExcellentMember(100L)).thenReturn(true);

        autoApprovalService.autoApproveAllPending();

        verify(hostWarningMapper).revokeVerification(100L);
        verify(hostWarningMapper).setDemotedAt(100L);
        verify(hostWarningMapper, never()).setPenaltyDate(anyLong(), any());
        verify(hostWarningMapper).resolveActiveWarnings(100L);
    }

    @Test
    @DisplayName("[A-06] 경고 3회 + 일반회원이면 penalted_at을 설정하고 경고를 소진한다")
    void limitReached_normal_penaltyDate() {
        when(certificationMapper.findAllPendingPostsForAutoApproval())
                .thenReturn(List.of(post(1, 11, 10, 100)));
        when(hostWarningMapper.countActiveWarnings(100L)).thenReturn(3);
        when(hostWarningMapper.isExcellentMember(100L)).thenReturn(false);

        autoApprovalService.autoApproveAllPending();

        verify(hostWarningMapper).setPenaltyDate(eq(100L), any());
        verify(hostWarningMapper, never()).revokeVerification(anyLong());
        verify(hostWarningMapper, never()).setDemotedAt(anyLong());
        verify(hostWarningMapper).resolveActiveWarnings(100L);
    }
}
