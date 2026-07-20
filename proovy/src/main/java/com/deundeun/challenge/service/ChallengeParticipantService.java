package com.deundeun.challenge.service;

import com.deundeun.challenge.domain.Challenge;
import com.deundeun.challenge.domain.ChallengeParticipant;
import com.deundeun.challenge.domain.ChallengeStatus;
import com.deundeun.challenge.domain.ParticipantStatus;
import com.deundeun.challenge.dto.response.ChallengeParticipantResponse;
import com.deundeun.challenge.mapper.ChallengeMapper;
import com.deundeun.challenge.mapper.ChallengeParticipantMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.service.WalletHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChallengeParticipantService {

    private final ChallengeMapper challengeMapper;
    private final ChallengeParticipantMapper challengeParticipantMapper;
    private final WalletHoldService walletHoldService;

    @Transactional
    public ChallengeParticipantResponse join(Long challengeId, Long userId) {
        // 행 잠금: 정원 확인과 참가자 등록 사이에 다른 참가 요청이 끼어드는 경쟁 조건 방지
        Challenge challenge = challengeMapper.findByIdForUpdate(challengeId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        if (challenge.getStatus() != ChallengeStatus.RECRUITING) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_RECRUITING);
        }
        // 탈퇴 이력이 있어도(challenge_id, user_id) 유니크 제약 때문에 재참가는 불가능하다
        if (challengeParticipantMapper.findByChallengeIdAndUserId(challengeId, userId) != null) {
            throw new ApiException(ErrorCode.ALREADY_JOINED_CHALLENGE);
        }
        if (challengeParticipantMapper.countActiveParticipants(challengeId) >= challenge.getMaxParticipants()) {
            throw new ApiException(ErrorCode.CHALLENGE_FULL);
        }

        if (challenge.getEntryFee() > 0) {
            walletHoldService.hold(userId, challenge.getEntryFee(), challengeId);
        }

        ChallengeParticipant participant = new ChallengeParticipant();
        participant.setChallengeId(challengeId);
        participant.setUserId(userId);
        participant.setStatus(ParticipantStatus.ACTIVE);
        challengeParticipantMapper.insert(participant);

        // joined_at은 DB 기본값(now())으로 채워지므로 다시 조회해서 응답에 담는다
        ChallengeParticipant saved = challengeParticipantMapper.findByChallengeIdAndUserId(challengeId, userId);
        return ChallengeParticipantResponse.from(saved);
    }

    @Transactional
    public void leave(Long challengeId, Long userId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        if (challenge.getHostId().equals(userId)) {
            throw new ApiException(ErrorCode.HOST_CANNOT_LEAVE);
        }

        // 행 잠금: 중복 탈퇴 요청이 홀딩을 두 번 취소하는 경쟁 조건 방지
        ChallengeParticipant participant =
                challengeParticipantMapper.findByChallengeIdAndUserIdForUpdate(challengeId, userId);
        if (participant == null || participant.getStatus() != ParticipantStatus.ACTIVE) {
            throw new ApiException(ErrorCode.NOT_CHALLENGE_PARTICIPANT);
        }

        challengeParticipantMapper.updateStatus(participant.getId(), ParticipantStatus.WITHDRAWN, LocalDateTime.now());

        // 홀딩 해제는 챌린지 시작 전(RECRUITING)에만 허용된다 — 시작 후 탈퇴는 참가비를 몰수한다
        if (challenge.getStatus() == ChallengeStatus.RECRUITING && challenge.getEntryFee() > 0) {
            walletHoldService.cancel(userId, challenge.getEntryFee(), challengeId);
        }
    }
}
