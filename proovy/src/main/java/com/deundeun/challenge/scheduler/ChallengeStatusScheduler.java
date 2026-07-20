package com.deundeun.challenge.scheduler;

import com.deundeun.challenge.domain.Challenge;
import com.deundeun.challenge.service.ChallengeCompletionService;
import com.deundeun.challenge.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 챌린지 상태(모집중 → 진행중 → 종료)를 매일 자정 직후 일괄 전환한다.
 * 상태+날짜 조건으로만 대상을 고르기 때문에, 서버가 중간에 꺼져있어 하루를 걸렀어도
 * 다음 실행에서 누락된 건까지 자동으로 함께 처리된다.
 *
 * 종료 처리는 챌린지 하나씩 {@link ChallengeCompletionService}의 별도 트랜잭션으로 호출한다 —
 * 여러 챌린지를 하나의 트랜잭션으로 묶으면 한 챌린지의 실패가 다른 챌린지의 정산까지 롤백시키기 때문이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeStatusScheduler {

    private final ChallengeService challengeService;
    private final ChallengeCompletionService challengeCompletionService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void transitionChallengeStatuses() {
        challengeService.startDueChallenges();

        for (Challenge challenge : challengeService.findChallengesToComplete()) {
            try {
                challengeCompletionService.completeChallenge(challenge);
            } catch (Exception e) {
                // 한 챌린지의 종료 처리 실패가 나머지 챌린지 처리를 막지 않게 한다.
                // 여기서 못 끝난 챌린지는 상태가 그대로 IN_PROGRESS라 다음 스케줄러 실행에서 다시 시도된다.
                log.error("챌린지 종료 처리 실패: challengeId={}", challenge.getId(), e);
            }
        }
    }
}
