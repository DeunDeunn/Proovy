package com.deundeun.mypage.service;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.domain.UserVerification;
import com.deundeun.auth.domain.UserVerificationStatus;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.auth.mapper.UserVerificationMapper;
import com.deundeun.challenge.dto.response.ChallengeSummaryResponse;
import com.deundeun.follow.mapper.FollowMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.mypage.dto.response.MyPageResponse;
import com.deundeun.mypage.mapper.MyPageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final UserVerificationMapper userVerificationMapper;
    private final MyPageMapper myPageMapper;

    public MyPageResponse getMyPage(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        long followerCount = followMapper.countFollowers(userId);
        long followingCount = followMapper.countFollowing(userId);

        UserVerification verification = userVerificationMapper.findLatestByUserId(userId);
        UserVerificationStatus verificationStatus = verification != null ? verification.getStatus() : null;
        boolean verified = verificationStatus == UserVerificationStatus.APPROVED;

        List<ChallengeSummaryResponse> participatingChallenges = myPageMapper.findParticipatingChallenges(userId);
        List<ChallengeSummaryResponse> hostingChallenges = myPageMapper.findHostingChallenges(userId);
        List<ChallengeSummaryResponse> completedChallenges = myPageMapper.findCompletedChallenges(userId);

        return new MyPageResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                verified,
                verificationStatus,
                user.getCreatedAt(),
                followerCount,
                followingCount,
                participatingChallenges,
                hostingChallenges,
                completedChallenges);
    }
}
