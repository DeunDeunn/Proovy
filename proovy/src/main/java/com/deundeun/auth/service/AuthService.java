package com.deundeun.auth.service;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.dto.UserMeResponse;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.security.jwt.JwtProvider;
import com.deundeun.global.security.jwt.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    public String reissueAccessToken(String refreshToken) {
        if (refreshToken == null || !jwtProvider.isValid(refreshToken)) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        if (!refreshTokenRepository.matches(userId, refreshToken)) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        return jwtProvider.createAccessToken(user.getId(), user.getRole());
    }

    public UserMeResponse getMe(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return UserMeResponse.from(user);
    }
}
