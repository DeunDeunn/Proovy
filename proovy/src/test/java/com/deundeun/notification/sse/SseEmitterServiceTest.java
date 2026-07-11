package com.deundeun.notification.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@DisplayName("SseEmitterService")
@ExtendWith(MockitoExtension.class)
class SseEmitterServiceTest {

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @InjectMocks
    private SseEmitterService sseEmitterService;

    @Test
    @DisplayName("구독 시 SseEmitter를 생성해 반환한다")
    void subscribe_returnsEmitter() {
        Long userId = 1L;

        SseEmitter emitter = sseEmitterService.subscribe(userId);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("구독 시 반환한 emitter를 레지스트리에 저장한다")
    void subscribe_savesEmitterToRepository() {
        Long userId = 1L;

        SseEmitter emitter = sseEmitterService.subscribe(userId);

        verify(sseEmitterRepository).save(eq(userId), eq(emitter));
    }
}
