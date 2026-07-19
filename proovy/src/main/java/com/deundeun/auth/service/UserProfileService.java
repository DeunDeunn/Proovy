package com.deundeun.auth.service;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.domain.UserVerification;
import com.deundeun.auth.domain.UserVerificationStatus;
import com.deundeun.auth.dto.response.OtherUserProfileResponse;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.auth.mapper.UserVerificationMapper;
import com.deundeun.follow.mapper.FollowMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final UserVerificationMapper userVerificationMapper;

    public OtherUserProfileResponse getProfile(Long viewerId, Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        long followerCount = followMapper.countFollowers(userId);
        long followingCount = followMapper.countFollowing(userId);
        boolean following = followMapper.exists(viewerId, userId);

        UserVerification verification = userVerificationMapper.findLatestByUserId(userId);
        boolean verified = verification != null && verification.getStatus() == UserVerificationStatus.APPROVED;

        return new OtherUserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                verified,
                followerCount,
                followingCount,
                following);
    }
}
