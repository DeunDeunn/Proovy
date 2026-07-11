package com.deundeun.auth.service;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.dto.UserMeResponse;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.security.jwt.JwtProvider;
import com.deundeun.global.security.jwt.RefreshToken;
import com.deundeun.global.security.jwt.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public record TokenPair(String accessToken, String refreshToken) {}

    public TokenPair reissueTokens(String refreshToken) {
        if (refreshToken == null || !jwtProvider.isValid(refreshToken)) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        if (!refreshTokenRepository.matches(userId, refreshToken)) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        User user = getUserOrThrow(userId);

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(new RefreshToken(user.getId(), newRefreshToken, Duration.ofMillis(refreshTokenExpiration)));

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    public UserMeResponse getMe(Long userId) {
        User user = getUserOrThrow(userId);
        return UserMeResponse.from(user);
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
