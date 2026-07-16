package com.deundeun.auth.service;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.domain.UserVerification;
import com.deundeun.auth.domain.UserVerificationStatus;
import com.deundeun.auth.dto.UserVerificationListItem;
import com.deundeun.auth.dto.response.UserVerificationListResponse;
import com.deundeun.auth.dto.response.UserVerificationStatusResponse;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.auth.mapper.UserVerificationMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserVerificationService {

    private static final int MIN_SUCCESSFUL_CHALLENGES = 20;

    private final UserVerificationMapper userVerificationMapper;
    private final UserMapper userMapper;

    public void apply(Long userId) {
        User user = getUserOrThrow(userId);
        if (userVerificationMapper.existsPending(userId)) {
            throw new ApiException(ErrorCode.VERIFICATION_ALREADY_PENDING);
        }
        long successCount = userVerificationMapper.countSuccessfulChallenges(userId, user.getDemotedAt());
        if (successCount < MIN_SUCCESSFUL_CHALLENGES) {
            throw new ApiException(ErrorCode.VERIFICATION_INELIGIBLE);
        }
        try {
            userVerificationMapper.insert(userId);
        } catch (DataIntegrityViolationException e) {
            if (!isUniqueViolation(e)) {
                throw e;
            }
            throw new ApiException(ErrorCode.VERIFICATION_ALREADY_PENDING);
        }
    }

    public UserVerificationStatusResponse getMyStatus(Long userId) {
        UserVerification latest = userVerificationMapper.findLatestByUserId(userId);
        if (latest == null) {
            return new UserVerificationStatusResponse(null, null, null, null);
        }
        return new UserVerificationStatusResponse(
                latest.getStatus(), latest.getAppliedAt(), latest.getApprovedAt(), latest.getRejectionReason());
    }

    public UserVerificationListResponse getList(UserVerificationStatus status, int page, int size) {
        long offset = (long) page * size;
        List<UserVerificationListItem> content = userVerificationMapper.findByStatus(status, size, offset);
        long totalElements = userVerificationMapper.countByStatus(status);
        return UserVerificationListResponse.of(content, page, size, totalElements);
    }

    public void review(Long id, UserVerificationStatus newStatus, String rejectionReason) {
        if (newStatus != UserVerificationStatus.APPROVED && newStatus != UserVerificationStatus.REJECTED) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        UserVerification verification = userVerificationMapper.findById(id);
        if (verification == null) {
            throw new ApiException(ErrorCode.VERIFICATION_NOT_FOUND);
        }
        if (newStatus == UserVerificationStatus.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new ApiException(ErrorCode.VERIFICATION_REJECTION_REASON_REQUIRED);
        }

        LocalDateTime approvedAt = newStatus == UserVerificationStatus.APPROVED ? LocalDateTime.now() : null;
        String reason = newStatus == UserVerificationStatus.REJECTED ? rejectionReason : null;
        int updated = userVerificationMapper.updateStatus(id, newStatus, approvedAt, reason);
        if (updated == 0) {
            throw new ApiException(ErrorCode.VERIFICATION_ALREADY_PROCESSED);
        }
    }

    private boolean isUniqueViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getRootCause();
        return cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState());
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
