package com.deundeun.global.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomHandshakeHandlerTest {

    private final CustomHandshakeHandler handler = new CustomHandshakeHandler();

    @Test
    @DisplayName("userId 속성으로 StompPrincipal을 생성한다")
    void determineUser_withUserIdAttribute_returnsStompPrincipal() {
        Map<String, Object> attributes = Map.of(JwtHandshakeInterceptor.USER_ID_ATTRIBUTE, 42L);

        Principal principal = handler.determineUser(null, null, attributes);

        assertThat(principal).isInstanceOf(StompPrincipal.class);
        assertThat(principal.getName()).isEqualTo("42");
    }
}
