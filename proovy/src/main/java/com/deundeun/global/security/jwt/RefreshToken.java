package com.deundeun.global.security.jwt;

import lombok.Getter;

import java.time.Duration;

@Getter
public class RefreshToken {

    private static final String KEY_PREFIX = "RT:";

    private final Long userId;
    private final String token;
    private final Duration ttl;

    public RefreshToken(Long userId, String token, Duration ttl) {
        this.userId = userId;
        this.token = token;
        this.ttl = ttl;
    }

    public String key() {
        return KEY_PREFIX + userId;
    }
}
