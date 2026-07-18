package com.deundeun.auth.dto;

import com.deundeun.auth.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserMeResponse {
    private Long id;
    private String provider;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;

    public static UserMeResponse from(User user) {
        return UserMeResponse.builder()
                .id(user.getId())
                .provider(user.getProvider())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .build();
    }
}
