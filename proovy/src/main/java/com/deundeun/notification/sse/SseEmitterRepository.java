package com.deundeun.notification.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseEmitterRepository {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void save(Long userId, SseEmitter emitter) {
        emitters.compute(userId, (key, list) -> {
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
            }

            list.add(emitter);
            log.debug("[Notification] SSE Emitter 저장: userId={}, count={}", userId, list.size());

            return list;
        });
    }

    public List<SseEmitter> findAllByUserId(Long userId) {
        return emitters.getOrDefault(userId, List.of());
    }

    public void remove(Long userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (key, list) -> {
            list.remove(emitter);
            log.debug("[Notification] SSE Emitter 제거: userId={}, count={}", userId, list.size());

            return list.isEmpty() ? null : list;
        });
    }
}
