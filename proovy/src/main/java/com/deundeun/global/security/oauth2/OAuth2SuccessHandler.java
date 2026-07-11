package com.deundeun.global.security.oauth2;

import com.deundeun.global.security.jwt.CustomUserDetails;
import com.deundeun.global.security.jwt.JwtProvider;
import com.deundeun.global.security.jwt.RefreshToken;
import com.deundeun.global.security.jwt.RefreshTokenRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

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
}
