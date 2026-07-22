package com.deundeun.auth.dto;

import java.util.regex.Pattern;

import com.deundeun.auth.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserMeResponse {
    // OAuthUserProcessor.generateTempNickname()이 만드는 값과 동일한 패턴.
    // 실제 사용자는 닉네임 규칙(2~10자)상 이 패턴을 만들 수 없어, 별도 컬럼 없이 이 값으로
    // "아직 닉네임을 설정하지 않음(프로필 미완료)"을 판단한다.
    private static final Pattern TEMP_NICKNAME = Pattern.compile("^user_[0-9a-f]{8}$");

    private Long id;
    private String provider;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;
    private boolean profileIncomplete;

    public static UserMeResponse from(User user) {
        return UserMeResponse.builder()
                .id(user.getId())
                .provider(user.getProvider())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .profileIncomplete(TEMP_NICKNAME.matcher(user.getNickname()).matches())
                .build();
    }
}
