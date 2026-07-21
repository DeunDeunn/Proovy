package com.deundeun.auth.service;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.dto.ConnectStatusResponse;
import com.deundeun.auth.dto.NicknameUpdateResponse;
import com.deundeun.auth.dto.ProfileImageUpdateResponse;
import com.deundeun.auth.dto.UserMeResponse;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.global.file.FileCategory;
import com.deundeun.global.file.TransactionalFileUploader;
import com.deundeun.global.security.jwt.JwtProvider;
import com.deundeun.global.security.jwt.ReauthTokenRepository;
import com.deundeun.global.security.jwt.RefreshToken;
import com.deundeun.global.security.jwt.RefreshTokenRepository;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ReauthTokenRepository reauthTokenRepository;
    private final UserMapper userMapper;
    private final WalletMapper walletMapper;
    private final TransactionalFileUploader transactionalFileUploader;

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

    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    public void checkNicknameDuplicate(String nickname) {
        String normalized = normalizeNickname(nickname);
        if (userMapper.existsByNickname(normalized, null)) {
            throw new ApiException(ErrorCode.NICKNAME_DUPLICATE);
        }
    }

    public NicknameUpdateResponse updateNickname(Long userId, String nickname) {
        String normalized = normalizeNickname(nickname);
        if (userMapper.existsByNickname(normalized, userId)) {
            throw new ApiException(ErrorCode.NICKNAME_DUPLICATE);
        }
        try {
            userMapper.updateNickname(userId, normalized);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.NICKNAME_DUPLICATE);
        }
        return new NicknameUpdateResponse(normalized);
    }

    @Transactional
    public ProfileImageUpdateResponse updateProfileImage(Long userId, MultipartFile image) {
        User user = getUserOrThrow(userId);
        String oldUrl = user.getProfileImageUrl();

        String newUrl = transactionalFileUploader.uploadReplacing(image, FileCategory.PROFILE, oldUrl);
        int updated = userMapper.updateProfileImageUrl(userId, newUrl);
        if (updated == 0) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        return new ProfileImageUpdateResponse(newUrl);
    }

    private String normalizeNickname(String nickname) {
        String trimmed = nickname == null ? null : nickname.trim();
        if (trimmed == null || trimmed.length() < 2 || trimmed.length() > 10) {
            throw new ApiException(ErrorCode.NICKNAME_INVALID);
        }
        return trimmed;
    }

    public ConnectStatusResponse getConnectStatus(Long userId, String provider) {
        User user = getUserOrThrow(userId);
        boolean connected = user.getProvider().equalsIgnoreCase(provider);
        return new ConnectStatusResponse(connected, connected ? user.getEmail() : null);
    }

    public void startReauth(Long userId) {
        reauthTokenRepository.markPending(userId);
    }

    public void withdraw(Long userId) {
        if (!reauthTokenRepository.isVerified(userId)) {
            throw new ApiException(ErrorCode.REAUTH_FAILED);
        }

        User user = getUserOrThrow(userId);

        LocalDateTime now = LocalDateTime.now();
        if (user.getSuspendedUntil() != null && now.isBefore(user.getSuspendedUntil())
                && (user.getSuspendedFrom() == null || !now.isBefore(user.getSuspendedFrom()))) {
            throw new ApiException(ErrorCode.WITHDRAWAL_SUSPENDED);
        }

        if (userMapper.existsActiveChallengeParticipation(userId)) {
            throw new ApiException(ErrorCode.WITHDRAWAL_ACTIVE_CHALLENGE);
        }

        Wallet wallet = walletMapper.selectByUserId(userId);
        if (wallet != null && wallet.getChargedBalance() > 0) {
            throw new ApiException(ErrorCode.WITHDRAWAL_CASH_REMAINING);
        }

        userMapper.softDelete(userId);
        refreshTokenRepository.deleteByUserId(userId);
        reauthTokenRepository.clearVerified(userId);
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
