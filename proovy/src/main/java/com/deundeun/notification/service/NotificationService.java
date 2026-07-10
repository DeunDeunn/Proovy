package com.deundeun.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deundeun.notification.domain.Notification;
import com.deundeun.notification.dto.NotificationCreateCommand;
import com.deundeun.notification.mapper.NotificationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    @Transactional
    public void create(NotificationCreateCommand command) {
        Notification notification = Notification.create(command);

        notificationMapper.insert(notification);
    }
}
