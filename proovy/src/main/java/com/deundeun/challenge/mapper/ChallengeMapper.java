package com.deundeun.challenge.mapper;

import java.util.List;

import com.deundeun.challenge.domain.Challenge;
import com.deundeun.challenge.domain.ChallengeSort;
import com.deundeun.challenge.domain.ChallengeStatus;
import com.deundeun.challenge.dto.response.ChallengeDetailResponse;
import com.deundeun.challenge.dto.response.ChallengeProgressResponse;
import com.deundeun.challenge.dto.response.ChallengeSummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChallengeMapper {
    void insert(Challenge challenge);

    List<Challenge> findByIds(@Param("ids") List<Long> ids);

    List<ChallengeSummaryResponse> search(
            @Param("categoryId") Long categoryId,
            @Param("status") ChallengeStatus status,
            @Param("keyword") String keyword,
            @Param("sort") ChallengeSort sort,
            @Param("offset") long offset,
            @Param("size") int size);

    long countBySearch(
            @Param("categoryId") Long categoryId,
            @Param("status") ChallengeStatus status,
            @Param("keyword") String keyword);

    List<ChallengeSummaryResponse> findMyChallenges(@Param("userId") Long userId);

    ChallengeDetailResponse findDetailById(@Param("id") Long id);

    ChallengeProgressResponse findProgressById(@Param("id") Long id);

    Challenge findById(@Param("id") Long id);

    Challenge findByIdForUpdate(@Param("id") Long id);

    int countActiveParticipantsExceptHost(@Param("challengeId") Long challengeId);

    void update(Challenge challenge);

    void updateStatus(@Param("id") Long id, @Param("status") ChallengeStatus status);

    /**
     * 모집중이면서 시작일이 지난 챌린지를 전부 진행중으로 전환한다 (스케줄러용, 매번 조건 검사라 누락분도 자동 처리됨).
     */
    void startDueChallenges();

    /**
     * 진행중이면서 종료일이 지난, 아직 종료 처리 안 된 챌린지 목록 (스케줄러용).
     */
    List<Challenge> findChallengesToComplete();

}
