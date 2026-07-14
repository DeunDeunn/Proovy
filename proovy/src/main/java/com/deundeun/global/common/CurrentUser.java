package com.deundeun.global.common;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.security.jwt.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUser {

    private CurrentUser() {
    }

    /**
     * @throws ApiException UNAUTHORIZED — 인증되지 않은 요청(비로그인, 만료된 토큰 등)일 때.
     * SecurityConfig가 이 엔드포인트를 permitAll로 열어둔 상태여도 안전하게 401로 응답한다.
     */
    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
