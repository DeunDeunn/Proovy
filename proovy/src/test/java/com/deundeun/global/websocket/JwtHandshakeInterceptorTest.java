package com.deundeun.global.websocket;

import com.deundeun.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtHandshakeInterceptorTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("쿠키 없이 CONNECT하면 핸드셰이크가 거부된다")
    void connect_withoutAccessTokenCookie_isRejected() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        assertThatThrownBy(() ->
                stompClient.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                        .get(5, TimeUnit.SECONDS)
        ).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("유효한 accessToken 쿠키로 CONNECT하면 성공하고 Principal이 설정된다")
    void connect_withValidAccessTokenCookie_succeedsAndSetsPrincipal() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        String token = jwtProvider.createAccessToken(1L, "USER");

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.add("Cookie", "accessToken=" + token);

        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", handshakeHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();
        assertThat(session.getSessionId()).isNotBlank();

        session.disconnect();
    }
}
