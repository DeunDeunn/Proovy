package com.deundeun.follow.dto;

import java.time.LocalDateTime;

public record FollowUserItem(
    Long userId,
    String nickname,
    String profileImageUrl,
    LocalDateTime followedAt
) {
}
