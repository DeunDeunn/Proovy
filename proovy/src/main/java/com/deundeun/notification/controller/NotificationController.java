package com.deundeun.notification.controller;

import com.deundeun.notification.dto.response.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationPageResponse> getNotifications(
            @RequestParam Long userId, //TODO 인증 붙으면 로그인 사용자 ID로 대체
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        NotificationPageResponse notifications = notificationService.getNotifications(userId, page, size);

        return ApiResponse.success(notifications, "알림 목록 조회를 완료했습니다.");
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(@RequestParam Long userId) {
        //TODO 인증 붙으면 로그인 사용자 ID로 대체
        UnreadCountResponse response = notificationService.countUnread(userId);

        return ApiResponse.success(response, "읽지 않은 알림 개수 조회를 완료했습니다.");
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationReadResponse> markAsRead(
        @RequestParam Long userId, //TODO 인증 붙으면 로그인 사용자 ID로 대체
        @PathVariable Long notificationId
    ) {
        NotificationReadResponse response = notificationService.markAsRead(userId, notificationId);

        return ApiResponse.success(response, "알림을 읽음 처리했습니다.");
    }

    @PatchMapping("/read-all")
    public ApiResponse<NotificationReadAllResponse> markAllAsRead(@RequestParam Long userId) {
        //TODO 인증 붙으면 로그인 사용자 ID로 대체
        NotificationReadAllResponse response = notificationService.markAllAsRead(userId);

        return ApiResponse.success(response, "전체 알림을 읽음 처리했습니다.");
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<NotificationDeleteResponse> delete(
        @RequestParam Long userId, //TODO 인증 붙으면 로그인 사용자 ID로 대체
        @PathVariable Long notificationId
    ) {
        NotificationDeleteResponse response = notificationService.delete(userId, notificationId);

        return ApiResponse.success(response, "알림을 삭제했습니다.");
    }

    @DeleteMapping("/all")
    public ApiResponse<NotificationDeleteAllResponse> deleteAll(
        @RequestParam Long userId //TODO 인증 붙으면 로그인 사용자 ID로 대체
    ) {
        NotificationDeleteAllResponse response = notificationService.deleteAll(userId);

        return ApiResponse.success(response, "전체 알림을 삭제했습니다.");
    }
}
