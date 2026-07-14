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
            Long expectedUserId = Long.valueOf(reauthUid);
            if (userId.equals(expectedUserId) && reauthTokenRepository.isPending(expectedUserId)) {
                reauthTokenRepository.clearPending(expectedUserId);
                reauthTokenRepository.markVerified(expectedUserId);
                redirectUrl = frontendUrl + "/mypage/withdraw?reauth=true";
            } else {
                reauthTokenRepository.clearPending(expectedUserId);
                String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
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
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
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
