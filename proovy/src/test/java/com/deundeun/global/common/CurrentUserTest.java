package com.deundeun.global.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.security.jwt.CustomUserDetails;

class CurrentUserTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserId_authenticated_returnsUserIdFromCustomUserDetails() {
        CustomUserDetails userDetails = new CustomUserDetails(42L, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        assertThat(CurrentUser.getUserId()).isEqualTo(42L);
    }

    @Test
    void getUserId_noAuthentication_throwsUnauthorized() {
        assertThatThrownBy(CurrentUser::getUserId)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void getUserId_anonymousPrincipal_throwsUnauthorized() {
        // permitAll 엔드포인트로 비로그인 요청이 들어오면 Spring Security가 principal에
        // "anonymousUser" 문자열을 넣어둔다(CustomUserDetails가 아님) — 캐스팅 대신 401로 처리돼야 한다.
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("anonymousUser", null));

        assertThatThrownBy(CurrentUser::getUserId)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
