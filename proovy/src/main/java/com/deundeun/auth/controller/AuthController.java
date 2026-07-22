package com.deundeun.auth.controller;

import com.deundeun.auth.dto.ConnectStatusResponse;
import com.deundeun.auth.dto.NicknameDuplicateResponse;
import com.deundeun.auth.dto.NicknameUpdateRequest;
import com.deundeun.auth.dto.NicknameUpdateResponse;
import com.deundeun.auth.dto.ProfileImageUpdateResponse;
import com.deundeun.auth.dto.UserMeResponse;
import com.deundeun.auth.service.AuthService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.security.jwt.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${cookie.domain:}")
    private String cookieDomain;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, "refreshToken");
        AuthService.TokenPair tokens = authService.reissueTokens(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        addCookieTo(headers, "accessToken", tokens.accessToken(), accessTokenExpiration / 1000);
        addCookieTo(headers, "refreshToken", tokens.refreshToken(), refreshTokenExpiration / 1000);

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.success(null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUserId());
        return ResponseEntity.ok()
                .headers(expireAuthCookies())
                .body(ApiResponse.success(null));
    }

    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.withdraw(userDetails.getUserId());
        return ResponseEntity.ok()
                .headers(expireAuthCookies())
                .body(ApiResponse.success(null));
    }

    private HttpHeaders expireAuthCookies() {
        HttpHeaders headers = new HttpHeaders();
        addCookieTo(headers, "accessToken", "", 0);
        addCookieTo(headers, "refreshToken", "", 0);
        return headers;
    }

    // 로컬은 도메인 미지정(host-only)으로 두고, 운영만 서브도메인 간 쿠키 공유를 위해 상위 도메인을 지정한다
    // (www/api 서브도메인이 분리돼 있고 WebSocket은 프록시를 못 타서 api 서브도메인에 직접 붙기 때문)
    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds);
        if (!cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    private void addCookieTo(HttpHeaders headers, String name, String value, long maxAgeSeconds) {
        // 도메인 쿠키로 전환하기 전 host-only로 심겨 있던 예전 쿠키가 같이 남아 중복 전송되지 않도록 먼저 만료시킨다
        if (!cookieDomain.isBlank()) {
            ResponseCookie hostOnlyExpire = ResponseCookie.from(name, "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            headers.add(HttpHeaders.SET_COOKIE, hostOnlyExpire.toString());
        }
        headers.add(HttpHeaders.SET_COOKIE, buildCookie(name, value, maxAgeSeconds).toString());
    }

    private void addCookieTo(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        if (!cookieDomain.isBlank()) {
            ResponseCookie hostOnlyExpire = ResponseCookie.from(name, "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, hostOnlyExpire.toString());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(name, value, maxAgeSeconds).toString());
    }

    private static final Set<String> REAUTH_PROVIDERS = Set.of("google", "kakao");

    @GetMapping("/{provider}/reauth")
    public void reauth(@PathVariable String provider,
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        HttpServletResponse response) throws IOException {
        if (!REAUTH_PROVIDERS.contains(provider)) {
            throw new ApiException(ErrorCode.OAUTH_INVALID_REQUEST);
        }

        authService.startReauth(userDetails.getUserId());

        addCookieTo(response, "reauthUid", String.valueOf(userDetails.getUserId()), 600);

        response.sendRedirect("/api/oauth2/authorization/" + provider);
    }

    @GetMapping("/connect/{provider}")
    public ResponseEntity<ApiResponse<ConnectStatusResponse>> connectStatus(@PathVariable String provider,
                                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!REAUTH_PROVIDERS.contains(provider)) {
            throw new ApiException(ErrorCode.OAUTH_INVALID_REQUEST);
        }
        ConnectStatusResponse response = authService.getConnectStatus(userDetails.getUserId(), provider);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/nickname/duplicate")
    public ResponseEntity<ApiResponse<NicknameDuplicateResponse>> checkNickname(@RequestParam String nickname) {
        authService.checkNicknameDuplicate(nickname);
        return ResponseEntity.ok(ApiResponse.success(new NicknameDuplicateResponse(true)));
    }

    @PatchMapping("/profile/nickname")
    public ResponseEntity<ApiResponse<NicknameUpdateResponse>> updateNickname(@RequestBody NicknameUpdateRequest request,
                                                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        NicknameUpdateResponse response = authService.updateNickname(userDetails.getUserId(), request.nickname());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/profile/image")
    public ResponseEntity<ApiResponse<ProfileImageUpdateResponse>> updateProfileImage(
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProfileImageUpdateResponse response = authService.updateProfileImage(userDetails.getUserId(), image);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/profile/image")
    public ResponseEntity<ApiResponse<Void>> deleteProfileImage(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.deleteProfileImage(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserMeResponse response = authService.getMe(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
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
