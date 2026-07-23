package com.deundeun.certification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 자정(KST)에 미검수 PENDING 인증글이 있는 방의 방장에게 경고를 적립하는 스케줄러.
 * 게시물 상태는 변경하지 않는다.
 * (단일 인스턴스 운영 가정 — pay 도메인 스케줄러와 동일)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingReviewWarningScheduler {

    private final PendingReviewWarningService pendingReviewWarningService;

    // cron: 초 분 시 일 월 요일 → 매일 00:00:00, zone으로 서버 시간대와 무관하게 KST 고정
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runMidnightPendingReviewWarning() {
        try {
            pendingReviewWarningService.warnForPendingReviews();
        } catch (Exception e) {
            // 실패해도 다음 날 자정에 다시 도니까 로그만 남김 (미처리 PENDING은 다음 실행이 다시 잡음)
            log.error("미검수 경고 배치 실패", e);
        }
    }
}
