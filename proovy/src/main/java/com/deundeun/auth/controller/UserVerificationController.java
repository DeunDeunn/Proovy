package com.deundeun.auth.controller;

import com.deundeun.auth.domain.UserVerificationStatus;
import com.deundeun.auth.dto.UserVerificationReviewRequest;
import com.deundeun.auth.dto.response.UserVerificationListResponse;
import com.deundeun.auth.dto.response.UserVerificationStatusResponse;
import com.deundeun.auth.service.UserVerificationService;
import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class UserVerificationController {

    private final UserVerificationService userVerificationService;

    @PostMapping("/api/user-verifications")
    public ApiResponse<Void> apply() {
        Long userId = CurrentUser.getUserId();
        userVerificationService.apply(userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/api/user-verifications/status")
    public ApiResponse<UserVerificationStatusResponse> getMyStatus() {
        Long userId = CurrentUser.getUserId();
        return ApiResponse.success(userVerificationService.getMyStatus(userId));
    }

    @GetMapping("/api/admin/user-verifications")
    public ApiResponse<UserVerificationListResponse> getList(
            @RequestParam(required = false) UserVerificationStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        CurrentUser.requireAdmin();
        return ApiResponse.success(userVerificationService.getList(status, page, size));
    }

    @PatchMapping("/api/admin/user-verifications/{id}")
    public ApiResponse<Void> review(@PathVariable Long id, @RequestBody UserVerificationReviewRequest request) {
        CurrentUser.requireAdmin();
        userVerificationService.review(id, request.status(), request.rejectionReason());
        return ApiResponse.success(null);
    }
}
