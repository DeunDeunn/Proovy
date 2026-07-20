package com.deundeun.mypage.dto.response;

import com.deundeun.auth.domain.UserVerificationStatus;
import com.deundeun.challenge.dto.response.ChallengeSummaryResponse;

import java.util.List;

public record MyPageResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        boolean verified,
        UserVerificationStatus verificationStatus,
        long followerCount,
        long followingCount,
        List<ChallengeSummaryResponse> participatingChallenges,
        List<ChallengeSummaryResponse> hostingChallenges
) {
}
