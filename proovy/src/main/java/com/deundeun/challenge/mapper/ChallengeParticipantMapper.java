package com.deundeun.challenge.mapper;

import com.deundeun.challenge.domain.ChallengeParticipant;
import com.deundeun.challenge.domain.ParticipantResult;
import com.deundeun.challenge.domain.ParticipantResultCandidate;
import com.deundeun.challenge.domain.ParticipantStatus;
import com.deundeun.challenge.dto.response.ChallengeParticipantListItemResponse;
import com.deundeun.challenge.dto.response.ChallengeParticipantManageResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChallengeParticipantMapper {

    void insert(ChallengeParticipant participant);

    List<ChallengeParticipantListItemResponse> findAllByChallengeId(@Param("challengeId") Long challengeId);

    ChallengeParticipant findByChallengeIdAndUserId(
            @Param("challengeId") Long challengeId,
            @Param("userId") Long userId);

    /**
     * 행 잠금 조회: 탈퇴 처리 중 동시 요청(중복 탈퇴)이 끼어드는 경쟁 조건을 방지한다.
     */
    ChallengeParticipant findByChallengeIdAndUserIdForUpdate(
            @Param("challengeId") Long challengeId,
            @Param("userId") Long userId);

    int countActiveParticipants(@Param("challengeId") Long challengeId);

    List<Long> findActiveUserIdsByChallengeId(@Param("challengeId") Long challengeId);

    void updateStatus(
            @Param("id") Long id,
            @Param("status") ParticipantStatus status,
            @Param("leftAt") LocalDateTime leftAt);

    /**
     * 챌린지 취소 시 활동중이던 참가자 전원을 한 번에 WITHDRAWN 처리한다.
     */
    void withdrawAllActiveByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("leftAt") LocalDateTime leftAt);

    /**
     * 챌린지 종료 처리 시 성공/실패 판정용: 활동중인 참가자별 승인된 인증글 개수.
     */
    List<ParticipantResultCandidate> findActiveParticipantsWithApprovedCount(
            @Param("challengeId") Long challengeId);

    void updateResult(@Param("id") Long id, @Param("result") ParticipantResult result);

    /**
     * 방장 관리 화면용: 참가자별 인증 승인 일수(진행률)와 검수 대기 건수.
     */
    List<ChallengeParticipantManageResponse> findParticipantsForManage(@Param("challengeId") Long challengeId);
}
