package com.deundeun.notification.controller;

import com.deundeun.notification.domain.NotificationCategory;
import com.deundeun.notification.dto.response.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.deundeun.global.common.ApiResponse;
import com.deundeun.global.common.CurrentUser;
import com.deundeun.notification.service.NotificationService;
import com.deundeun.notification.sse.SseEmitterService;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(
        @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    ) {
        Long userId = CurrentUser.getUserId();
        SseEmitter emitter = sseEmitterService.subscribe(userId, lastEventId);

        return ResponseEntity.ok()
            .header("X-Accel-Buffering", "no")
            .header(HttpHeaders.CACHE_CONTROL, "no-cache")
            .body(emitter);
    }

    @GetMapping
    public ApiResponse<NotificationPageResponse> getNotifications(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) NotificationCategory category
    ) {
        Long userId = CurrentUser.getUserId();
        NotificationPageResponse notifications = notificationService.getNotifications(userId, page, size, category);

        return ApiResponse.success(notifications, "알림 목록 조회를 완료했습니다.");
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        Long userId = CurrentUser.getUserId();
        UnreadCountResponse response = notificationService.countUnread(userId);

        return ApiResponse.success(response, "읽지 않은 알림 개수 조회를 완료했습니다.");
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationReadResponse> markAsRead(@PathVariable Long notificationId) {
        Long userId = CurrentUser.getUserId();
        NotificationReadResponse response = notificationService.markAsRead(userId, notificationId);

        return ApiResponse.success(response, "알림을 읽음 처리했습니다.");
    }

    @PatchMapping("/read-all")
    public ApiResponse<NotificationReadAllResponse> markAllAsRead() {
        Long userId = CurrentUser.getUserId();
        NotificationReadAllResponse response = notificationService.markAllAsRead(userId);

        return ApiResponse.success(response, "전체 알림을 읽음 처리했습니다.");
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<NotificationDeleteResponse> delete(@PathVariable Long notificationId) {
        Long userId = CurrentUser.getUserId();
        NotificationDeleteResponse response = notificationService.delete(userId, notificationId);

        return ApiResponse.success(response, "알림을 삭제했습니다.");
    }

    @DeleteMapping("/all")
    public ApiResponse<NotificationDeleteAllResponse> deleteAll() {
        Long userId = CurrentUser.getUserId();
        NotificationDeleteAllResponse response = notificationService.deleteAll(userId);

        return ApiResponse.success(response, "전체 알림을 삭제했습니다.");
    }
}
