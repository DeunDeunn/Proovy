package com.deundeun.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.notification.domain.Notification;
import com.deundeun.notification.domain.NotificationCategory;
import com.deundeun.notification.domain.NotificationType;
import com.deundeun.notification.domain.TargetType;
import com.deundeun.notification.dto.NotificationCreateCommand;
import com.deundeun.notification.dto.response.NotificationDeleteAllResponse;
import com.deundeun.notification.dto.response.NotificationDeleteResponse;
import com.deundeun.notification.dto.response.NotificationPageResponse;
import com.deundeun.notification.dto.response.NotificationReadAllResponse;
import com.deundeun.notification.dto.response.NotificationReadResponse;
import com.deundeun.notification.dto.response.NotificationResponse;
import com.deundeun.notification.dto.response.UnreadCountResponse;
import com.deundeun.notification.event.NotificationCreatedEvent;
import com.deundeun.notification.mapper.NotificationMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;

@DisplayName("NotificationService")
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("커맨드로 생성한 알림을 저장한다")
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
        assertThat(saved.getCreatedAt()).isNotNull();

        ArgumentCaptor<NotificationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(command.userId());
        assertThat(eventCaptor.getValue().notification().title()).isEqualTo(saved.getTitle());
    }

    @Test
    @DisplayName("중복 이벤트키로 저장이 무시되면 알림 생성 이벤트를 발행하지 않는다")
    void create_doesNotPublish_whenDuplicateEventKey() {
        NotificationCreateCommand command = new NotificationCreateCommand(
                1L,
                NotificationType.VERIFICATION_APPROVED,
                "인증이 승인되었습니다.",
                "아침 운동 챌린지 인증이 승인되었습니다.",
                TargetType.VERIFICATION_POST,
                15L,
                "VERIFICATION_APPROVED:15"
        );
        doThrow(DuplicateKeyException.class).when(notificationMapper).insert(any(Notification.class));

        notificationService.create(command);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("조회한 알림 목록을 페이지 응답으로 매핑한다")
    void getNotifications_mapsNotificationsToPageResponse() {
        Long userId = 1L;
        Notification n1 = Notification.create(new NotificationCreateCommand(
                userId, NotificationType.VERIFICATION_APPROVED, "제목1", "내용1",
                TargetType.VERIFICATION_POST, 10L, "KEY1"));
        Notification n2 = Notification.create(new NotificationCreateCommand(
                userId, NotificationType.SETTLEMENT_COMPLETED, "제목2", "내용2",
                TargetType.SETTLEMENT, 20L, "KEY2"));

        when(notificationMapper.findByUserId(userId, null, 20, 0)).thenReturn(List.of(n1, n2));
        when(notificationMapper.countByUserId(userId, null)).thenReturn(25);

        NotificationPageResponse result = notificationService.getNotifications(userId, 0, 20, null);

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
    @DisplayName("0부터 시작하는 page를 offset으로 변환한다")
    void getNotifications_convertsZeroBasedPageToOffset() {
        Long userId = 1L;
        when(notificationMapper.findByUserId(userId, null, 20, 40)).thenReturn(List.of());
        when(notificationMapper.countByUserId(userId, null)).thenReturn(0);

        notificationService.getNotifications(userId, 2, 20, null);

        verify(notificationMapper).findByUserId(userId, null, 20, 40);
    }

    @Test
    @DisplayName("마지막 페이지에서는 hasNext가 false다")
    void getNotifications_hasNextFalseOnLastPage() {
        Long userId = 1L;
        when(notificationMapper.findByUserId(userId, null, 20, 20)).thenReturn(List.of());
        when(notificationMapper.countByUserId(userId, null)).thenReturn(25);

        NotificationPageResponse result = notificationService.getNotifications(userId, 1, 20, null);

        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("category가 주어지면 해당 카테고리에 속한 타입 목록으로 필터링한다")
    void getNotifications_withCategory_filtersByCategoryTypes() {
        Long userId = 1L;
        when(notificationMapper.findByUserId(eq(userId), any(), eq(20), eq(0))).thenReturn(List.of());
        when(notificationMapper.countByUserId(eq(userId), any())).thenReturn(0);

        notificationService.getNotifications(userId, 0, 20, NotificationCategory.SETTLEMENT);

        verify(notificationMapper).findByUserId(userId, NotificationCategory.SETTLEMENT.getTypes(), 20, 0);
        verify(notificationMapper).countByUserId(userId, NotificationCategory.SETTLEMENT.getTypes());
    }

    @Test
    @DisplayName("lastEventId 이후의 알림을 limit 개수만큼 조회해 응답으로 매핑한다")
    void getNotificationsAfter_mapsNotificationsWithinLimit() {
        Long userId = 1L;
        Notification n1 = Notification.create(new NotificationCreateCommand(
                userId, NotificationType.VERIFICATION_APPROVED, "제목1", "내용1",
                TargetType.VERIFICATION_POST, 10L, "KEY1"));
        when(notificationMapper.findByUserIdAfterId(userId, 3L, 100)).thenReturn(List.of(n1));

        List<NotificationResponse> result = notificationService.getNotificationsAfter(userId, 3L, 100);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("제목1");
        verify(notificationMapper).findByUserIdAfterId(userId, 3L, 100);
    }

    @Test
    @DisplayName("안 읽은 알림 개수를 Mapper에서 그대로 반환한다")
    void countUnreadCount_returnsFromMapper() {
        Long userId = 1L;
        when(notificationMapper.countUnread(userId)).thenReturn(5);

        UnreadCountResponse result = notificationService.countUnread(userId);

        assertThat(result.unreadCount()).isEqualTo(5);
        verify(notificationMapper).countUnread(userId);
    }

    @Test
    @DisplayName("아직 안 읽은 알림은 읽음 처리하고 새 readAt을 반환한다")
    void markAsRead_updatesAndReturnsNewReadAt_whenNotYetRead() {
        Long userId = 1L;
        Long notificationId = 10L;
        Notification notification = mock(Notification.class);
        when(notification.getUserId()).thenReturn(userId);
        when(notification.getReadAt()).thenReturn(null);
        when(notificationMapper.findById(notificationId)).thenReturn(Optional.of(notification));

        NotificationReadResponse result = notificationService.markAsRead(userId, notificationId);

        assertThat(result.id()).isEqualTo(notificationId);
        assertThat(result.readAt()).isNotNull();
        verify(notificationMapper).markAsRead(eq(notificationId), eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("이미 읽은 알림은 갱신 없이 기존 readAt을 그대로 반환한다")
    void markAsRead_returnsExistingReadAt_whenAlreadyRead() {
        Long userId = 1L;
        Long notificationId = 10L;
        LocalDateTime existingReadAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        Notification notification = mock(Notification.class);
        when(notification.getId()).thenReturn(notificationId);
        when(notification.getUserId()).thenReturn(userId);
        when(notification.getReadAt()).thenReturn(existingReadAt);
        when(notificationMapper.findById(notificationId)).thenReturn(Optional.of(notification));

        NotificationReadResponse result = notificationService.markAsRead(userId, notificationId);

        assertThat(result.id()).isEqualTo(notificationId);
        assertThat(result.readAt()).isEqualTo(existingReadAt);
        verify(notificationMapper, never()).markAsRead(any(), any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 알림을 읽음 처리하면 404 예외를 던진다")
    void markAsRead_throwsNotFound_whenNotificationMissing() {
        Long userId = 1L;
        Long notificationId = 99L;
        when(notificationMapper.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(userId, notificationId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);

        verify(notificationMapper, never()).markAsRead(any(), any(), any());
    }

    @Test
    @DisplayName("본인 알림이 아니면 403 예외를 던진다")
    void markAsRead_throwsForbidden_whenRequesterIsNotOwner() {
        Long ownerUserId = 1L;
        Long requesterUserId = 2L;
        Long notificationId = 10L;
        Notification notification = mock(Notification.class);
        when(notification.getUserId()).thenReturn(ownerUserId);
        when(notificationMapper.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(requesterUserId, notificationId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_FORBIDDEN);

        verify(notificationMapper, never()).markAsRead(any(), any(), any());
    }

    @Test
    @DisplayName("안 읽은 알림을 전체 읽음 처리하고 갱신 개수를 반환한다")
    void markAllAsRead_updatesUnreadNotificationsAndReturnsCount() {
        Long userId = 1L;
        when(notificationMapper.markAllAsRead(eq(userId), any(LocalDateTime.class))).thenReturn(3);

        NotificationReadAllResponse result = notificationService.markAllAsRead(userId);

        assertThat(result.updatedCount()).isEqualTo(3);
        assertThat(result.readAt()).isNotNull();
        verify(notificationMapper).markAllAsRead(eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("읽음 처리할 알림이 없으면 updatedCount 0을 반환한다")
    void markAllAsRead_returnsZeroCount_whenNothingToUpdate() {
        Long userId = 1L;
        when(notificationMapper.markAllAsRead(eq(userId), any(LocalDateTime.class))).thenReturn(0);

        NotificationReadAllResponse result = notificationService.markAllAsRead(userId);

        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.readAt()).isNotNull();
    }

    @Test
    @DisplayName("본인 알림을 삭제하고 deletedAt을 반환한다")
    void delete_softDeletesAndReturnsDeletedAt() {
        Long userId = 1L;
        Long notificationId = 10L;
        Notification notification = mock(Notification.class);
        when(notification.getUserId()).thenReturn(userId);
        when(notificationMapper.findById(notificationId)).thenReturn(Optional.of(notification));

        NotificationDeleteResponse result = notificationService.delete(userId, notificationId);

        assertThat(result.id()).isEqualTo(notificationId);
        assertThat(result.deletedAt()).isNotNull();
        verify(notificationMapper).delete(eq(notificationId), eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("존재하지 않거나 이미 삭제된 알림을 삭제하면 404 예외를 던진다")
    void delete_throwsNotFound_whenNotificationMissingOrAlreadyDeleted() {
        Long userId = 1L;
        Long notificationId = 99L;
        when(notificationMapper.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.delete(userId, notificationId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);

        verify(notificationMapper, never()).delete(any(), any(), any());
    }

    @Test
    @DisplayName("본인 알림이 아니면 삭제 시 403 예외를 던진다")
    void delete_throwsForbidden_whenRequesterIsNotOwner() {
        Long ownerUserId = 1L;
        Long requesterUserId = 2L;
        Long notificationId = 10L;
        Notification notification = mock(Notification.class);
        when(notification.getUserId()).thenReturn(ownerUserId);
        when(notificationMapper.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.delete(requesterUserId, notificationId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_FORBIDDEN);

        verify(notificationMapper, never()).delete(any(), any(), any());
    }

    @Test
    @DisplayName("삭제되지 않은 전체 알림을 삭제하고 삭제 개수를 반환한다")
    void deleteAll_deletesUndeletedNotificationsAndReturnsCount() {
        Long userId = 1L;
        when(notificationMapper.deleteAll(eq(userId), any(LocalDateTime.class))).thenReturn(4);

        NotificationDeleteAllResponse result = notificationService.deleteAll(userId);

        assertThat(result.deletedCount()).isEqualTo(4);
        assertThat(result.deletedAt()).isNotNull();
        verify(notificationMapper).deleteAll(eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("삭제할 알림이 없으면 deletedCount 0을 반환한다")
    void deleteAll_returnsZeroCount_whenNothingToDelete() {
        Long userId = 1L;
        when(notificationMapper.deleteAll(eq(userId), any(LocalDateTime.class))).thenReturn(0);

        NotificationDeleteAllResponse result = notificationService.deleteAll(userId);

        assertThat(result.deletedCount()).isEqualTo(0);
        assertThat(result.deletedAt()).isNotNull();
    }
}
