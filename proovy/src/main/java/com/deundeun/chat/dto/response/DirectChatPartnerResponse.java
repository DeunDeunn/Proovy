package com.deundeun.chat.dto.response;

import com.deundeun.auth.domain.User;

public record DirectChatPartnerResponse(
    Long userId,
    String nickname,
    String profileImageUrl,
    boolean badgeApproved
) {

    public static DirectChatPartnerResponse of(User user) {
        return new DirectChatPartnerResponse(
            user.getId(),
            user.getNickname(),
            user.getProfileImageUrl(),
            false
        );
    }
}
