package com.deundeun.notification.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deundeun.notification.domain.Notification;
import com.deundeun.notification.dto.NotificationCreateCommand;
import com.deundeun.notification.dto.response.NotificationPageResponse;
import com.deundeun.notification.dto.response.NotificationResponse;
import com.deundeun.notification.mapper.NotificationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
            log.info("[Notification] 알림 저장: userId={}, type={}, eventKey={}",
                    command.userId(), command.type(), command.eventKey());
        } catch (DuplicateKeyException e) {
            log.info("[Notification] 중복 알림 무시: eventKey={}", command.eventKey());
        }
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(Long userId, int page, int size) {
        log.info("[Notification] 목록 조회: userId={}, page={}, size={}", userId, page, size);
        int offset = page * size;

        List<NotificationResponse> content = notificationMapper.findByUserId(userId, size, offset).stream()
                .map(NotificationResponse::from)
                .toList();
        long totalElements = notificationMapper.countByUserId(userId);

        return NotificationPageResponse.of(content, page, size, totalElements);
    }
}
