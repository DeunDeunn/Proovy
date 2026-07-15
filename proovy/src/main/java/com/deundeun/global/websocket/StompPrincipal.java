package com.deundeun.global.websocket;

import java.security.Principal;

public record StompPrincipal(Long userId) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
