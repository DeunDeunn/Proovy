package com.deundeun.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "RT:";

    private final StringRedisTemplate redisTemplate;

    public void save(RefreshToken refreshToken) {
        redisTemplate.opsForValue().set(
                refreshToken.key(),
                refreshToken.getToken(),
                refreshToken.getTtl()
        );
    }

    public boolean matches(Long userId, String token) {
        String saved = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return saved != null && saved.equals(token);
    }

    public void deleteByUserId(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
