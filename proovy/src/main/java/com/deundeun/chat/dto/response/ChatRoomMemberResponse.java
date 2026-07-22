package com.deundeun.chat.dto.response;

import com.deundeun.auth.domain.User;

public record ChatRoomMemberResponse(
    Long userId,
    String nickname,
    String profileImageUrl,
    boolean badgeApproved
) {

    public static ChatRoomMemberResponse of(User user, boolean badgeApproved) {
        return new ChatRoomMemberResponse(
            user.getId(),
            user.getNickname(),
            user.getProfileImageUrl(),
            badgeApproved
        );
    }
}
