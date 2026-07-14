package com.deundeun.global.common;

/**
 * TODO: JWT 인증 붙으면 이 클래스를 SecurityContextHolder에서 인증된 유저 ID를
 * 꺼내는 방식으로 교체할 것. 지금은 로그인이 구현되기 전이라 userId=1을 고정 반환한다.
 * 호출부(컨트롤러)는 getUserId() 시그니처만 유지되면 수정 없이 그대로 붙는다.
 */
public class CurrentUser {

    private static final Long TEMP_USER_ID = 1L;

    private CurrentUser() {
    }

    public static Long getUserId() {
        return TEMP_USER_ID;
    }
}
