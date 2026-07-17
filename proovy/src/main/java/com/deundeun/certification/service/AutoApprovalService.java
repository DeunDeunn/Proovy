package com.deundeun.certification.service;

import com.deundeun.certification.dto.PendingPostForAutoApproval;
import com.deundeun.certification.mapper.CertificationMapper;
import com.deundeun.certification.mapper.HostWarningMapper;
import com.deundeun.notification.event.VerificationApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

// 자정(KST) 자동 승인 배치의 실제 처리.
// 1) 미검수 PENDING 글 전부 자동 승인(approval_type=AUTO)
// 2) 해당 챌린지 방장에게 경고 적립(챌린지당 1건)
// 3) 경고 3회 누적 시: 우수회원=강등 / 일반회원=14일 개설 금지, 쓴 경고는 RESOLVED로 소진
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoApprovalService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int WARNING_LIMIT = 3;
    private static final int PENALTY_DAYS = 14;

    private final CertificationMapper certificationMapper;
    private final HostWarningMapper hostWarningMapper;
    private final ApplicationEventPublisher eventPublisher;   // 알림 이벤트 (AFTER_COMMIT 발송 — @Transactional 필수)

    @Transactional
    public void autoApproveAllPending() {
        List<PendingPostForAutoApproval> pendingPosts = certificationMapper.findAllPendingPostsForAutoApproval();
        if (pendingPosts.isEmpty()) {
            log.info("자동 승인 - 대상 없음");
            return;
        }

        // 1) 일괄 자동 승인
        List<Long> postIds = pendingPosts.stream().map(PendingPostForAutoApproval::getPostId).toList();
        int approvedCount = certificationMapper.approvePostsAuto(postIds);

        // 2) 방장 경고 적립 — 챌린지당 1건 (글이 여러 개여도 그 챌린지 경고는 1건)
        Map<Long, Long> hostByChallenge = new LinkedHashMap<>();   // challengeId -> hostId
        for (PendingPostForAutoApproval post : pendingPosts) {
            hostByChallenge.putIfAbsent(post.getChallengeId(), post.getHostId());
        }
        hostByChallenge.forEach((challengeId, hostId) -> hostWarningMapper.insertWarning(hostId, challengeId));

        // 3) 경고 3회 누적 페널티 — 방장별 1번만 검사 (챌린지 여러 개 방치했어도 판정은 1회)
        for (Long hostId : new LinkedHashSet<>(hostByChallenge.values())) {
            applyPenaltyIfNeeded(hostId);
        }

        // 4) 작성자에게 승인 알림 (수동 승인과 동일 이벤트)
        for (PendingPostForAutoApproval post : pendingPosts) {
            eventPublisher.publishEvent(new VerificationApprovedEvent(post.getAuthorId(), post.getPostId()));
        }

        log.info("자동 승인 완료 - 대상 {}건, 승인 {}건, 경고 {}건", pendingPosts.size(), approvedCount, hostByChallenge.size());
    }

    private void applyPenaltyIfNeeded(Long hostId) {
        int warningCount = hostWarningMapper.countActiveWarnings(hostId);
        if (warningCount < WARNING_LIMIT) {
            return;
        }
        if (hostWarningMapper.isExcellentMember(hostId)) {
            // 우수회원 → 일반회원 강등
            hostWarningMapper.revokeVerification(hostId);
            hostWarningMapper.setDemotedAt(hostId);
            log.info("경고 {}회 누적 - 우수회원 강등 - userId={}", warningCount, hostId);
        } else {
            // 일반회원 → 오늘(KST) 기준 14일 뒤까지 챌린지 개설 금지
            LocalDateTime penaltyUntil = LocalDate.now(KST).plusDays(PENALTY_DAYS).atStartOfDay();
            hostWarningMapper.setPenaltyDate(hostId, penaltyUntil);
            log.info("경고 {}회 누적 - {}까지 챌린지 개설 금지 - userId={}", warningCount, penaltyUntil, hostId);
        }
        // 페널티에 쓴 경고는 소진 → 다음 누적은 0부터
        hostWarningMapper.resolveActiveWarnings(hostId);
    }
}
