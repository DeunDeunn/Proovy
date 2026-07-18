package com.deundeun.auth.controller;

import com.deundeun.auth.dto.ConnectStatusResponse;
import com.deundeun.auth.dto.NicknameDuplicateResponse;
import com.deundeun.auth.dto.NicknameUpdateRequest;
import com.deundeun.auth.dto.NicknameUpdateResponse;
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

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, "refreshToken");
        AuthService.TokenPair tokens = authService.reissueTokens(refreshToken);

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", tokens.accessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(accessTokenExpiration / 1000)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(refreshTokenExpiration / 1000)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

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
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return headers;
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

        ResponseCookie reauthCookie = ResponseCookie.from("reauthUid", String.valueOf(userDetails.getUserId()))
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(600)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, reauthCookie.toString());

        response.sendRedirect("/oauth2/authorization/" + provider);
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
