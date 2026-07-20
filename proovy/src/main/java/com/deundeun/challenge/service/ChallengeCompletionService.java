package com.deundeun.challenge.service;

import com.deundeun.challenge.domain.Challenge;
import com.deundeun.challenge.domain.ChallengeStatus;
import com.deundeun.challenge.domain.ParticipantResult;
import com.deundeun.challenge.domain.ParticipantResultCandidate;
import com.deundeun.challenge.mapper.ChallengeMapper;
import com.deundeun.challenge.mapper.ChallengeParticipantMapper;
import com.deundeun.pay.service.WalletSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 챌린지 1건의 종료 처리(참가자별 성공/실패 확정 + 정산 요청 + 상태 전환)를 전담한다.
 * 정산(settle) 호출과 result 업데이트 중 하나만 성공하는 상황을 막기 위해 이 메서드 전체를
 * 하나의 트랜잭션으로 묶는다 — 여러 챌린지를 순회하는 스케줄러와 트랜잭션 범위를 분리해서,
 * 챌린지 하나가 실패해도 다른 챌린지의 종료 처리에는 영향이 없게 한다.
 */
@Service
@RequiredArgsConstructor
public class ChallengeCompletionService {

    private final ChallengeMapper challengeMapper;
    private final ChallengeParticipantMapper challengeParticipantMapper;
    private final WalletSettlementService walletSettlementService;

    @Transactional
    public void completeChallenge(Challenge challenge) {
        int totalDays = (int) (challenge.getEndDate().toEpochDay() - challenge.getStartDate().toEpochDay()) + 1;

        List<Long> successUserIds = new ArrayList<>();
        List<Long> failUserIds = new ArrayList<>();
        for (ParticipantResultCandidate candidate :
                challengeParticipantMapper.findActiveParticipantsWithApprovedCount(challenge.getId())) {
            int successRate = (candidate.getApprovedCount() * 100) / totalDays;
            ParticipantResult result = successRate >= challenge.getSuccessCriteriaRate()
                    ? ParticipantResult.SUCCESS
                    : ParticipantResult.FAIL;
            challengeParticipantMapper.updateResult(candidate.getId(), result);

            if (result == ParticipantResult.SUCCESS) {
                successUserIds.add(candidate.getUserId());
            } else {
                failUserIds.add(candidate.getUserId());
            }
        }

        // TODO: 방장 자격 박탈(isHostDisqualified) 판단 기준이 아직 확정되지 않아 우선 false로 고정한다.
        walletSettlementService.settle(
                challenge.getId(), successUserIds, failUserIds, challenge.getHostId(), false, challenge.getEntryFee());

        challengeMapper.updateStatus(challenge.getId(), ChallengeStatus.COMPLETED);
    }
}
