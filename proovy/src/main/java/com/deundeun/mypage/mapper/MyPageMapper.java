package com.deundeun.mypage.mapper;

import com.deundeun.challenge.dto.response.ChallengeSummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MyPageMapper {

    List<ChallengeSummaryResponse> findParticipatingChallenges(@Param("userId") Long userId);

    List<ChallengeSummaryResponse> findHostingChallenges(@Param("userId") Long userId);
}
