package com.deundeun.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.notification.domain.Notification;
import com.deundeun.notification.domain.NotificationType;
import com.deundeun.notification.domain.TargetType;
import com.deundeun.notification.dto.NotificationCreateCommand;
import com.deundeun.notification.mapper.NotificationMapper;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void create_savesNotificationBuiltFromCommand() {
        NotificationCreateCommand command = new NotificationCreateCommand(
                1L,
                NotificationType.VERIFICATION_APPROVED,
                "인증이 승인되었습니다.",
                "아침 운동 챌린지 인증이 승인되었습니다.",
                TargetType.VERIFICATION_POST,
                15L,
                "VERIFICATION_APPROVED:15"
        );

        notificationService.create(command);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(command.userId());
        assertThat(saved.getType()).isEqualTo(command.type());
        assertThat(saved.getTitle()).isEqualTo(command.title());
        assertThat(saved.getContent()).isEqualTo(command.content());
        assertThat(saved.getTargetType()).isEqualTo(command.targetType());
        assertThat(saved.getTargetId()).isEqualTo(command.targetId());
        assertThat(saved.getEventKey()).isEqualTo(command.eventKey());
    }
}
