package com.deundeun.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
import com.deundeun.notification.dto.response.NotificationPageResponse;
import com.deundeun.notification.dto.response.UnreadCountResponse;
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

    @Test
    void getNotifications_mapsNotificationsToPageResponse() {
        Long userId = 1L;
        Notification n1 = Notification.create(new NotificationCreateCommand(
                userId, NotificationType.VERIFICATION_APPROVED, "제목1", "내용1",
                TargetType.VERIFICATION_POST, 10L, "KEY1"));
        Notification n2 = Notification.create(new NotificationCreateCommand(
                userId, NotificationType.SETTLEMENT_COMPLETED, "제목2", "내용2",
                TargetType.SETTLEMENT, 20L, "KEY2"));

        when(notificationMapper.findByUserId(userId, 20, 0)).thenReturn(List.of(n1, n2));
        when(notificationMapper.countByUserId(userId)).thenReturn(25);

        NotificationPageResponse result = notificationService.getNotifications(userId, 0, 20);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).title()).isEqualTo("제목1");
        assertThat(result.content().get(1).title()).isEqualTo("제목2");
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(25);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void getNotifications_convertsZeroBasedPageToOffset() {
        Long userId = 1L;
        when(notificationMapper.findByUserId(userId, 20, 40)).thenReturn(List.of());
        when(notificationMapper.countByUserId(userId)).thenReturn(0);

        notificationService.getNotifications(userId, 2, 20);

        verify(notificationMapper).findByUserId(userId, 20, 40);
    }

    @Test
    void getNotifications_hasNextFalseOnLastPage() {
        Long userId = 1L;
        when(notificationMapper.findByUserId(userId, 20, 20)).thenReturn(List.of());
        when(notificationMapper.countByUserId(userId)).thenReturn(25);

        NotificationPageResponse result = notificationService.getNotifications(userId, 1, 20);

        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void getUnreadCount_returnsCountFromMapper() {
        Long userId = 1L;
        when(notificationMapper.countUnread(userId)).thenReturn(5);

        UnreadCountResponse result = notificationService.getUnreadCount(userId);

        assertThat(result.unreadCount()).isEqualTo(5);
        verify(notificationMapper).countUnread(userId);
    }
}
