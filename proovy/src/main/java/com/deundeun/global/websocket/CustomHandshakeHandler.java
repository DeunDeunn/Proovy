package com.deundeun.global.websocket;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected @Nullable Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                                Map<String, Object> attributes) {
        Long userId = (Long) attributes.get(JwtHandshakeInterceptor.USER_ID_ATTRIBUTE);
        return new StompPrincipal(userId);
    }
}
