package com.deundeun.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class ReauthTokenRepository {

    private static final String PENDING_PREFIX = "REAUTH_PENDING:";
    private static final String VERIFIED_PREFIX = "REAUTH_VERIFIED:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public void markPending(Long userId) {
        redisTemplate.opsForValue().set(PENDING_PREFIX + userId, "1", TTL);
    }

    public boolean isPending(Long userId) {
        return redisTemplate.hasKey(PENDING_PREFIX + userId);
    }

    public void clearPending(Long userId) {
        redisTemplate.delete(PENDING_PREFIX + userId);
    }

    public void markVerified(Long userId) {
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + userId, "1", TTL);
    }

    public boolean isVerified(Long userId) {
        return redisTemplate.hasKey(VERIFIED_PREFIX + userId);
    }

    public void clearVerified(Long userId) {
        redisTemplate.delete(VERIFIED_PREFIX + userId);
    }
}
