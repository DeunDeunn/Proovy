package com.deundeun.global.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 회원탈퇴 재인증(/api/auth/{provider}/reauth) 흐름에서만 OAuth 인가 요청에
 * {@code prompt=login}을 추가해 재로그인(비밀번호 재입력)을 강제하는 리졸버.
 *
 * <p>일반 로그인 흐름까지 매번 재로그인을 강제하면 UX가 나빠지므로, 재인증 시작 시점에
 * {@link com.deundeun.auth.controller.AuthController#reauth}가 심어두는 {@code reauthUid}
 * 쿠키가 있을 때만 prompt를 추가한다. 그 외 요청(일반 로그인)은 기본 리졸버 동작 그대로다.
 */
public class ReauthAwareAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String REAUTH_COOKIE_NAME = "reauthUid";
    private static final String PROMPT_PARAM_NAME = "prompt";
    private static final String PROMPT_LOGIN_VALUE = "login";

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public ReauthAwareAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
                                                     String authorizationRequestBaseUri) {
        this.defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return forceLoginPromptIfReauth(defaultResolver.resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return forceLoginPromptIfReauth(defaultResolver.resolve(request, clientRegistrationId), request);
    }

    private OAuth2AuthorizationRequest forceLoginPromptIfReauth(OAuth2AuthorizationRequest authorizationRequest,
                                                                  HttpServletRequest request) {
        if (authorizationRequest == null || !isReauthRequest(request)) {
            return authorizationRequest;
        }

        Map<String, Object> additionalParameters = new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());
        additionalParameters.put(PROMPT_PARAM_NAME, PROMPT_LOGIN_VALUE);

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }

    private boolean isReauthRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (REAUTH_COOKIE_NAME.equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }
}
