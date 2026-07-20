package com.deundeun.follow.controller;

import com.deundeun.follow.dto.response.FollowListResponse;
import com.deundeun.follow.dto.response.FollowStatusResponse;
import com.deundeun.follow.service.FollowService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}/follow")
    public ApiResponse<Void> follow(@PathVariable Long userId) {
        Long followerId = CurrentUser.getUserId();
        followService.follow(followerId, userId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{userId}/follow")
    public ApiResponse<Void> unfollow(@PathVariable Long userId) {
        Long followerId = CurrentUser.getUserId();
        followService.unfollow(followerId, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{userId}/followers")
    public ApiResponse<FollowListResponse> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(followService.getFollowers(userId, page, size));
    }

    @GetMapping("/{userId}/following")
    public ApiResponse<FollowListResponse> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(followService.getFollowing(userId, page, size));
    }

    @GetMapping("/{userId}/follow/status")
    public ApiResponse<FollowStatusResponse> getFollowStatus(@PathVariable Long userId) {
        Long followerId = CurrentUser.getUserId();
        return ApiResponse.success(followService.getFollowStatus(followerId, userId));
    }
}
