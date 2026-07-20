package com.deundeun.challenge.mapper;

import java.util.List;

import com.deundeun.challenge.domain.Challenge;
import com.deundeun.challenge.domain.ChallengeStatus;
import com.deundeun.challenge.dto.response.ChallengeDetailResponse;
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
            @Param("offset") long offset,
            @Param("size") int size);

    long countBySearch(
            @Param("categoryId") Long categoryId,
            @Param("status") ChallengeStatus status,
            @Param("keyword") String keyword);

    List<ChallengeSummaryResponse> findMyChallenges(@Param("userId") Long userId);

    ChallengeDetailResponse findDetailById(@Param("id") Long id);

    Challenge findById(@Param("id") Long id);

    Challenge findByIdForUpdate(@Param("id") Long id);

    int countActiveParticipantsExceptHost(@Param("challengeId") Long challengeId);

    void update(Challenge challenge);

    void updateStatus(@Param("id") Long id, @Param("status") ChallengeStatus status);

}
