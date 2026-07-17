package com.deundeun.certification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 자정(KST)에 미검수 PENDING 인증글을 자동 승인하는 스케줄러.
 * 실제 처리(승인·경고·페널티)는 AutoApprovalService가 트랜잭션으로 수행한다.
 * (단일 인스턴스 운영 가정 — pay 도메인 스케줄러와 동일)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoApprovalScheduler {

    private final AutoApprovalService autoApprovalService;

    // cron: 초 분 시 일 월 요일 → 매일 00:00:00, zone으로 서버 시간대와 무관하게 KST 고정
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runMidnightAutoApproval() {
        try {
            autoApprovalService.autoApproveAllPending();
        } catch (Exception e) {
            // 실패해도 다음 날 자정에 다시 도니까 로그만 남김 (미처리 PENDING은 다음 실행이 다시 잡음)
            log.error("자동 승인 배치 실패", e);
        }
    }
}
