package com.deundeun.follow.service;

import com.deundeun.auth.domain.User;
import com.deundeun.auth.mapper.UserMapper;
import com.deundeun.follow.dto.FollowUserItem;
import com.deundeun.follow.dto.response.FollowListResponse;
import com.deundeun.follow.dto.response.FollowStatusResponse;
import com.deundeun.follow.mapper.FollowMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new ApiException(ErrorCode.SELF_FOLLOW_NOT_ALLOWED);
        }
        getUserOrThrow(followingId);
        if (followMapper.exists(followerId, followingId)) {
            throw new ApiException(ErrorCode.ALREADY_FOLLOWING);
        }
        try {
            followMapper.insert(followerId, followingId);
        } catch (DataIntegrityViolationException e) {
            if (!isUniqueViolation(e)) {
                throw e;
            }
            throw new ApiException(ErrorCode.ALREADY_FOLLOWING);
        }
    }

    public void unfollow(Long followerId, Long followingId) {
        getUserOrThrow(followingId);
        int deleted = followMapper.delete(followerId, followingId);
        if (deleted == 0) {
            throw new ApiException(ErrorCode.NOT_FOLLOWING);
        }
    }

    public FollowListResponse getFollowers(Long userId, int page, int size) {
        getUserOrThrow(userId);
        long offset = (long) page * size;
        List<FollowUserItem> content = followMapper.findFollowers(userId, size, offset);
        long totalElements = followMapper.countFollowers(userId);
        return FollowListResponse.of(content, page, size, totalElements);
    }

    public FollowListResponse getFollowing(Long userId, int page, int size) {
        getUserOrThrow(userId);
        long offset = (long) page * size;
        List<FollowUserItem> content = followMapper.findFollowing(userId, size, offset);
        long totalElements = followMapper.countFollowing(userId);
        return FollowListResponse.of(content, page, size, totalElements);
    }

    private boolean isUniqueViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getRootCause();
        return cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState());
    }

    public FollowStatusResponse getFollowStatus(Long followerId, Long followingId) {
        getUserOrThrow(followingId);
        boolean following = followMapper.exists(followerId, followingId);
        return new FollowStatusResponse(following);
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
