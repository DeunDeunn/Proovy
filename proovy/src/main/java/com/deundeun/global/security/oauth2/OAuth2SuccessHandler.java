package com.deundeun.global.security.oauth2;

import com.deundeun.global.security.jwt.CustomUserDetails;
import com.deundeun.global.security.jwt.JwtProvider;
import com.deundeun.global.security.jwt.ReauthTokenRepository;
import com.deundeun.global.security.jwt.RefreshToken;
import com.deundeun.global.security.jwt.RefreshTokenRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ReauthTokenRepository reauthTokenRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${cookie.domain:}")
    private String cookieDomain;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String role = userDetails.getRole();

        String reauthUid = extractCookie(request, "reauthUid");
        if (reauthUid != null) {
            expireCookie(response, "reauthUid");

            String redirectUrl;
            String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            try {
                Long expectedUserId = Long.valueOf(reauthUid);
                if (userId.equals(expectedUserId) && reauthTokenRepository.isPending(expectedUserId)) {
                    reauthTokenRepository.clearPending(expectedUserId);
                    reauthTokenRepository.markVerified(expectedUserId);
                    redirectUrl = frontendUrl + "/mypage/withdraw?reauth=true";
                } else {
                    redirectUrl = frontendUrl + "/mypage/withdraw?reauth=false&error=" + provider + "_reauth_failed";
                }
            } catch (NumberFormatException e) {
                redirectUrl = frontendUrl + "/mypage/withdraw?reauth=false&error=" + provider + "_reauth_failed";
            }

            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        String redirectUrl;
        try {
            String accessToken = jwtProvider.createAccessToken(userId, role);
            String refreshToken = jwtProvider.createRefreshToken(userId);

            refreshTokenRepository.save(new RefreshToken(userId, refreshToken, Duration.ofMillis(refreshTokenExpiration)));

            addCookie(response, "accessToken", accessToken, accessTokenExpiration / 1000);
            addCookie(response, "refreshToken", refreshToken, refreshTokenExpiration / 1000);

            redirectUrl = userDetails.isNewUser()
                    ? frontendUrl + "/auth/complete-profile?success=true"
                    : frontendUrl + "/auth/callback?success=true";
        } catch (Exception e) {
            redirectUrl = frontendUrl + "/auth/callback?success=false";
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        // 로컬은 도메인 미지정(host-only)으로 두고, 운영만 서브도메인 간 쿠키 공유를 위해 상위 도메인을 지정한다
        // (www/api 서브도메인이 분리돼 있고 WebSocket은 프록시를 못 타서 api 서브도메인에 직접 붙기 때문)
        if (!cookieDomain.isBlank()) {
            // 도메인 쿠키로 전환하기 전 host-only로 심겨 있던 예전 쿠키가 같이 남아 중복 전송되지 않도록 먼저 만료시킨다
            ResponseCookie hostOnlyExpire = ResponseCookie.from(name, "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.addHeader("Set-Cookie", hostOnlyExpire.toString());
        }

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds);
        if (!cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        response.addHeader("Set-Cookie", builder.build().toString());
    }

    private void expireCookie(HttpServletResponse response, String name) {
        addCookie(response, name, "", 0);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
