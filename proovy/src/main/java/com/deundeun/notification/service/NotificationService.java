package com.deundeun.notification.service;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.notification.domain.Notification;
import com.deundeun.notification.dto.NotificationCreateCommand;
import com.deundeun.notification.dto.response.*;
import com.deundeun.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    @Transactional
    public void create(NotificationCreateCommand command) {
        Notification notification = Notification.create(command);

        try {
            notificationMapper.insert(notification);
            log.info("[Notification] 알림 생성 완료: eventKey={}", command.eventKey());
        } catch (DuplicateKeyException e) {
            log.debug("[Notification] 중복 알림 무시: eventKey={}", command.eventKey());
        }
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(Long userId, int page, int size) {
        log.debug("[Notification] 목록 조회: userId={}, page={}, size={}", userId, page, size);

        int offset = page * size;
        List<NotificationResponse> content = notificationMapper.findByUserId(userId, size, offset).stream()
                .map(NotificationResponse::from)
                .toList();
        long totalElements = notificationMapper.countByUserId(userId);

        return NotificationPageResponse.of(content, page, size, totalElements);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread(Long userId) {
        log.debug("[Notification] 안 읽은 개수 조회: userId={}", userId);

        int unreadCount = notificationMapper.countUnread(userId);
        return new UnreadCountResponse(unreadCount);
    }

    @Transactional
    public NotificationReadResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = getNotification(notificationId);

        validateOwner(notification, userId);

        if (notification.getReadAt() != null) {
            return alreadyReadResponse(notification);
        }

        LocalDateTime readAt = LocalDateTime.now();
        notificationMapper.markAsRead(notificationId, userId, readAt);
        log.info("[Notification] 읽음 처리 완료: notificationId={}, userId={}, readAt={}", notificationId, userId, readAt);

        return new NotificationReadResponse(notificationId, readAt);
    }


    @Transactional
    public NotificationReadAllResponse markAllAsRead(Long userId) {
        LocalDateTime readAt = LocalDateTime.now();
        int updatedCount = notificationMapper.markAllAsRead(userId, readAt);
        log.info("[Notification] 전체 읽음 처리 완료: userId={}, updatedCount={}, readAt={}", userId, updatedCount, readAt);

        return new NotificationReadAllResponse(updatedCount, readAt);
    }

    @Transactional
    public NotificationDeleteResponse delete(Long userId, Long notificationId) {
        Notification notification = getNotification(notificationId);

        validateOwner(notification, userId);

        LocalDateTime deletedAt = LocalDateTime.now();
        notificationMapper.delete(notificationId, userId, deletedAt);
        log.info("[Notification] 삭제 완료: notificationId={}, userId={}, deletedAt={}", notificationId, userId, deletedAt);

        return new NotificationDeleteResponse(notificationId, deletedAt);
    }

    @Transactional
    public NotificationDeleteAllResponse deleteAll(Long userId) {
        LocalDateTime deletedAt = LocalDateTime.now();
        int deletedCount = notificationMapper.deleteAll(userId, deletedAt);
        log.info("[Notification] 전체 삭제 완료: userId={}, deletedCount={}, deletedAt={}", userId, deletedCount, deletedAt);

        return new NotificationDeleteAllResponse(deletedCount, deletedAt);
    }

    public Notification getNotification(Long notificationId) {
        return notificationMapper.findById(notificationId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    private void validateOwner(Notification notification, Long userId) {
        if (!notification.getUserId().equals(userId)) {
            log.warn("[Notification] 권한 없는 접근 시도: notificationId={}, requestUserId={}, ownerUserId={}",
                notification.getId(), userId, notification.getUserId());
            throw new ApiException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }
    }

    private NotificationReadResponse alreadyReadResponse(Notification notification) {
        log.debug("[Notification] 이미 읽은 알림 재요청: notificationId={}", notification.getId());
        return new NotificationReadResponse(notification.getId(), notification.getReadAt());
    }
}
