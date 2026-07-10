package com.deundeun.notification.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.notification.dto.response.NotificationPageResponse;
import com.deundeun.notification.dto.response.UnreadCountResponse;
import com.deundeun.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications")
    public ApiResponse<NotificationPageResponse> getNotifications(
            @RequestParam Long userId, //TODO 인증 붙으면 로그인 사용자 ID로 대체
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        NotificationPageResponse notifications = notificationService.getNotifications(userId, page, size);

        return ApiResponse.success(notifications);
    }

    @GetMapping("/api/notifications/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(@RequestParam Long userId) {
        //TODO 인증 붙으면 로그인 사용자 ID로 대체
        UnreadCountResponse response = notificationService.getUnreadCount(userId);

        return ApiResponse.success(response);
    }
}
