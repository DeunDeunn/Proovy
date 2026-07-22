package com.deundeun.global.common;

import com.deundeun.challenge.domain.Challenge;
import com.deundeun.challenge.mapper.ChallengeMapper;
import com.deundeun.challenge.service.ChallengeCompletionService;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시연/테스트용 - 자정 스케줄러(ChallengeStatusScheduler)를 기다리지 않고
 * 특정 챌린지 하나만 즉시 종료 처리(성공/실패 판정 + 정산)한다.
 * 다른 챌린지에는 영향 없음. 시연 끝나면 제거할 것.
 */
@RestController
@RequiredArgsConstructor
public class DevSettlementTriggerController {

    private final ChallengeMapper challengeMapper;
    private final ChallengeCompletionService challengeCompletionService;

    @PostMapping("/api/dev/trigger-settlement/{challengeId}")
    public ApiResponse<Void> trigger(@PathVariable Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }

        challengeCompletionService.completeChallenge(challenge);
        return ApiResponse.success(null);
    }
}
