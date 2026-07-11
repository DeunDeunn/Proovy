package com.deundeun.notification.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.deundeun.notification.domain.NotificationType;
import com.deundeun.notification.domain.TargetType;
import com.deundeun.notification.dto.response.NotificationResponse;
import com.deundeun.notification.service.NotificationService;

@DisplayName("SseEmitterService")
@ExtendWith(MockitoExtension.class)
class SseEmitterServiceTest {

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Executor notificationExecutor;

    @InjectMocks
    private SseEmitterService sseEmitterService;

    @Test
    @DisplayName("구독 시 SseEmitter를 생성해 반환한다")
    void subscribe_returnsEmitter() {
        Long userId = 1L;

        SseEmitter emitter = sseEmitterService.subscribe(userId, null);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("구독 시 반환한 emitter를 레지스트리에 저장한다")
    void subscribe_savesEmitterToRepository() {
        Long userId = 1L;

        SseEmitter emitter = sseEmitterService.subscribe(userId, null);

        verify(sseEmitterRepository).save(eq(userId), eq(emitter));
    }

    @Test
    @DisplayName("Last-Event-ID가 있으면 그 이후의 미수신 알림을 재전송한다")
    void subscribe_resendsMissedNotifications_whenLastEventIdPresent() {
        Long userId = 1L;
        NotificationResponse missed = new NotificationResponse(
                5L, NotificationType.VERIFICATION_APPROVED, "제목", "내용",
                TargetType.VERIFICATION_POST, 10L, null, LocalDateTime.now());
        when(notificationService.getNotificationsAfter(userId, 3L)).thenReturn(List.of(missed));

        sseEmitterService.subscribe(userId, "3");

        verify(notificationService).getNotificationsAfter(userId, 3L);
    }

    @Test
    @DisplayName("Last-Event-ID가 없으면 미수신 알림 조회를 하지 않는다")
    void subscribe_doesNotQueryMissedNotifications_whenLastEventIdAbsent() {
        Long userId = 1L;

        sseEmitterService.subscribe(userId, null);

        verify(notificationService, never()).getNotificationsAfter(any(), any());
    }

    @Test
    @DisplayName("Last-Event-ID가 숫자가 아니면 미수신 알림 조회를 하지 않는다")
    void subscribe_doesNotQueryMissedNotifications_whenLastEventIdInvalid() {
        Long userId = 1L;

        sseEmitterService.subscribe(userId, "not-a-number");

        verify(notificationService, never()).getNotificationsAfter(any(), any());
    }

    @Test
    @DisplayName("하트비트 전송 시 등록된 emitter마다 실행기로 작업을 넘긴다")
    void sendHeartbeat_dispatchesOncePerEmitter() {
        SseEmitter emitter1 = new SseEmitter();
        SseEmitter emitter2 = new SseEmitter();
        when(sseEmitterRepository.findAll())
                .thenReturn(Map.of(1L, List.of(emitter1), 2L, List.of(emitter2)));

        sseEmitterService.sendHeartbeat();

        verify(notificationExecutor, times(2)).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("하트비트 전송이 성공하면 레지스트리에서 emitter를 제거하지 않는다")
    void sendHeartbeat_doesNotRemoveEmitter_whenSendSucceeds() {
        Long userId = 1L;
        SseEmitter emitter = new SseEmitter();
        when(sseEmitterRepository.findAll()).thenReturn(Map.of(userId, List.of(emitter)));
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(notificationExecutor).execute(any(Runnable.class));

        sseEmitterService.sendHeartbeat();

        verify(sseEmitterRepository, never()).remove(any(), any());
    }

    @Test
    @DisplayName("하트비트 전송이 실패하면 레지스트리에서 emitter를 제거한다")
    void sendHeartbeat_removesEmitter_whenSendFails() {
        Long userId = 1L;
        SseEmitter emitter = new SseEmitter();
        emitter.complete();
        when(sseEmitterRepository.findAll()).thenReturn(Map.of(userId, List.of(emitter)));
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(notificationExecutor).execute(any(Runnable.class));

        sseEmitterService.sendHeartbeat();

        verify(sseEmitterRepository).remove(userId, emitter);
    }

    @Test
    @DisplayName("구독 중인 emitter가 없으면 실행기로 아무 작업도 넘기지 않는다")
    void sendHeartbeat_doesNothing_whenNoEmittersRegistered() {
        when(sseEmitterRepository.findAll()).thenReturn(Map.of());

        sseEmitterService.sendHeartbeat();

        verify(notificationExecutor, never()).execute(any());
    }
}
