package com.deundeun.notification.sse;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.notification.dto.response.NotificationResponse;
import com.deundeun.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SseEmitterService {

    private static final Long DEFAULT_TIMEOUT = TimeUnit.HOURS.toMillis(1);
    private static final int MAX_RESEND_COUNT = 100;
    private final SseEmitterRepository sseEmitterRepository;
    private final NotificationService notificationService;
    private final Executor notificationExecutor;

    public SseEmitterService(
        SseEmitterRepository sseEmitterRepository,
        NotificationService notificationService,
        @Qualifier("notificationExecutor") Executor notificationExecutor
    ) {
        this.sseEmitterRepository = sseEmitterRepository;
        this.notificationService = notificationService;
        this.notificationExecutor = notificationExecutor;
    }

    public SseEmitter subscribe(Long userId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        registerCallbacks(emitter, userId);
        sseEmitterRepository.save(userId, emitter);
        sendConnectEvent(emitter, userId);
        resendMissedNotifications(emitter, userId, lastEventId);

        return emitter;
    }

    public void publish(Long userId, NotificationResponse notification) {
        for (SseEmitter emitter : sseEmitterRepository.findAllByUserId(userId)) {
            sendNotificationEvent(userId, emitter, notification);
        }
    }

    @Scheduled(fixedRate = 30_000)
    public void sendHeartbeat() {
        sseEmitterRepository.findAll().forEach((userId, emitterList) ->
            emitterList.forEach(emitter ->
                notificationExecutor.execute(() -> sendHeartbeatTo(userId, emitter))));
    }

    private void registerCallbacks(SseEmitter emitter, Long userId) {
        emitter.onCompletion(() -> {
            sseEmitterRepository.remove(userId, emitter);
            log.debug("[Notification] SSE 연결 종료: userId={}", userId);
        });

        emitter.onTimeout(() -> {
            sseEmitterRepository.remove(userId, emitter);
            emitter.complete();
            log.debug("[Notification] SSE 연결 타임아웃: userId={}", userId);
        });

        emitter.onError(e -> {
            sseEmitterRepository.remove(userId, emitter);
            log.warn("[Notification] SSE 연결 에러: userId={}, message={}", userId, e.getMessage());
        });
    }

    private void sendHeartbeatTo(Long userId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("HEARTBEAT").data(Map.of("message", "SSE 연결이 유지되고 있습니다.")));
        } catch (IOException | IllegalStateException e) {
            sseEmitterRepository.remove(userId, emitter);
            log.debug("[Notification] 하트비트 실패로 emitter 제거: userId={}, message={}", userId, e.getMessage());
        }
    }

    private void sendConnectEvent(SseEmitter emitter, Long userId) {
        try {
            emitter.send(SseEmitter.event()
                .name("CONNECT")
                .data(Map.of("message", "SSE 연결이 완료되었습니다.")));
        } catch (IOException | IllegalStateException e) {
            sseEmitterRepository.remove(userId, emitter);
            log.warn("[Notification] SSE 구독 초기화 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.NOTIFICATION_SUBSCRIBE_FAILED);
        }
    }

    private void resendMissedNotifications(SseEmitter emitter, Long userId, String lastEventId) {
        Long parsedLastEventId = parseLastEventId(lastEventId);
        if (parsedLastEventId == null) {
            return;
        }

        List<NotificationResponse> missed =
            notificationService.getNotificationsAfter(userId, parsedLastEventId, MAX_RESEND_COUNT);
        log.debug("[Notification] 재연결 시 미수신 알림 재전송: userId={}, lastEventId={}, count={}",
            userId, parsedLastEventId, missed.size());

        for (NotificationResponse notification : missed) {
            sendNotificationEvent(userId, emitter, notification);
        }
    }

    private Long parseLastEventId(String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(lastEventId);
        } catch (NumberFormatException e) {
            log.warn("[Notification] 잘못된 Last-Event-ID 무시: lastEventId={}", lastEventId);
            return null;
        }
    }

    private void sendNotificationEvent(Long userId, SseEmitter emitter, NotificationResponse notification) {
        try {
            emitter.send(SseEmitter.event()
                .id(String.valueOf(notification.id()))
                .name("NOTIFICATION_CREATED")
                .data(notification));
        } catch (IOException | IllegalStateException e) {
            sseEmitterRepository.remove(userId, emitter);
            log.debug("[Notification] SSE 전송 실패: userId={}, message={}", userId, e.getMessage());
        }
    }
}
