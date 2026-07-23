package com.deundeun.certification.service;

import com.deundeun.certification.mapper.HostWarningMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingReviewWarningProcessor {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int WARNING_LIMIT = 3;
    private static final int PENALTY_DAYS = 14;

    private final HostWarningMapper hostWarningMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processHost(Long hostId) {
        // INSERT 시점에도 PENDING인 챌린지만 DB가 선별한다.
        // 기존 DB CHECK 제약과의 호환을 위해 reason 값은 AUTO_APPROVAL을 유지한다.
        List<Long> warnings = hostWarningMapper.insertWarningsForPendingChallenges(hostId);
        if (warnings.isEmpty()) {
            return 0;
        }

        applyPenaltyIfNeeded(hostId);
        return warnings.size();
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
