package com.deundeun.auth.dto.response;

public record OtherUserProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        boolean verified,
        long followerCount,
        long followingCount,
        boolean following
) {
}
