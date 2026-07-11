package com.deundeun.notification.sse;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private static final Long DEFAULT_TIMEOUT = TimeUnit.HOURS.toMillis(1);
    private final SseEmitterRepository sseEmitterRepository;

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        registerCallbacks(emitter, userId);
        sseEmitterRepository.save(userId, emitter);
        sendConnectEvent(emitter, userId);

        return emitter;
    }

    public void publish(Long userId, NotificationResponse notification) {
        for (SseEmitter emitter : sseEmitterRepository.findAllByUserId(userId)) {
            try {
                emitter.send(SseEmitter.event()
                    .name("NOTIFICATION_CREATED")
                    .data(notification));
            } catch (IOException | IllegalStateException e) {
                sseEmitterRepository.remove(userId, emitter);
                log.debug("[Notification] SSE push 실패: userId={}, message={}", userId, e.getMessage());
            }
        }
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
            log.debug("[Notification] SSE 연결 에러: userId={}, message={}", userId, e.getMessage());
        });
    }

    private void sendConnectEvent(SseEmitter emitter, Long userId) {
        try {
            emitter.send(SseEmitter.event()
                .name("CONNECT")
                .data(Map.of("message", "SSE 연결이 완료되었습니다.")));
        } catch (IOException e) {
            sseEmitterRepository.remove(userId, emitter);
            log.warn("[Notification] SSE 구독 초기화 실패: userId={}", userId, e);
            throw new ApiException(ErrorCode.NOTIFICATION_SUBSCRIBE_FAILED);
        }
    }
}
