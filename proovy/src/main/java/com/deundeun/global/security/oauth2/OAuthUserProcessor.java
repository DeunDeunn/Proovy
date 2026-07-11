package com.deundeun.global.security.oauth2;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.security.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuthUserProcessor {

    private final UserMapper userMapper;

    public CustomUserDetails process(String registrationId, Map<String, Object> attributes) {
        String provider = registrationId.toUpperCase();
        String providerId = extractProviderId(registrationId, attributes);
        String email = extractEmail(registrationId, attributes);

        User user = userMapper.findByProviderAndProviderId(provider, providerId);
        boolean newUser = (user == null);

        if (newUser) {
            user = User.builder()
                    .provider(provider)
                    .providerId(providerId)
                    .email(email)
                    .nickname(generateTempNickname())
                    .role("USER")
                    .build();
            userMapper.insert(user);
        }

        return new CustomUserDetails(user.getId(), user.getRole(), newUser, attributes);
    }

    private String extractProviderId(String registrationId, Map<String, Object> attributes) {
        try {
            if ("google".equals(registrationId)) {
                return String.valueOf(attributes.get("sub"));
            }
            if ("kakao".equals(registrationId)) {
                return String.valueOf(attributes.get("id"));
            }
        } catch (Exception e) {
            throw profileFetchFailed(registrationId);
        }
        throw invalidProvider(registrationId);
    }

    @SuppressWarnings("unchecked")
    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        try {
            if ("google".equals(registrationId)) {
                return (String) attributes.get("email");
            }
            if ("kakao".equals(registrationId)) {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                return (String) kakaoAccount.get("email");
            }
        } catch (Exception e) {
            throw profileFetchFailed(registrationId);
        }
        throw invalidProvider(registrationId);
    }

    private OAuth2AuthenticationException profileFetchFailed(String registrationId) {
        ErrorCode errorCode = "google".equals(registrationId)
                ? ErrorCode.GOOGLE_PROFILE_FETCH_FAILED
                : ErrorCode.KAKAO_PROFILE_FETCH_FAILED;
        return new OAuth2AuthenticationException(
                new OAuth2Error(errorCode.getCode(), errorCode.getMessage(), null),
                errorCode.getMessage());
    }

    private OAuth2AuthenticationException invalidProvider(String registrationId) {
        return new OAuth2AuthenticationException(
                new OAuth2Error(ErrorCode.OAUTH_INVALID_REQUEST.getCode()),
                "지원하지 않는 로그인 provider입니다: " + registrationId);
    }

    private String generateTempNickname() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
