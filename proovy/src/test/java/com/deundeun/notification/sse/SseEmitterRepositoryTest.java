package com.deundeun.notification.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@DisplayName("SseEmitterRepository")
class SseEmitterRepositoryTest {

    private final SseEmitterRepository repository = new SseEmitterRepository();

    @Test
    @DisplayName("저장한 emitter를 userId로 조회할 수 있다")
    void save_thenFindAllByUserId_returnsSavedEmitter() {
        Long userId = 1L;
        SseEmitter emitter = new SseEmitter();

        repository.save(userId, emitter);

        assertThat(repository.findAllByUserId(userId)).containsExactly(emitter);
    }

    @Test
    @DisplayName("같은 userId로 여러 emitter를 저장하면 모두 조회된다")
    void save_multipleEmittersForSameUser_returnsAll() {
        Long userId = 1L;
        SseEmitter emitter1 = new SseEmitter();
        SseEmitter emitter2 = new SseEmitter();

        repository.save(userId, emitter1);
        repository.save(userId, emitter2);

        assertThat(repository.findAllByUserId(userId)).containsExactlyInAnyOrder(emitter1, emitter2);
    }

    @Test
    @DisplayName("저장된 적 없는 userId를 조회하면 빈 리스트를 반환한다")
    void findAllByUserId_returnsEmptyList_whenNoEmitterSaved() {
        assertThat(repository.findAllByUserId(999L)).isEmpty();
    }

    @Test
    @DisplayName("emitter를 제거하면 해당 userId의 목록에서 사라진다")
    void remove_removesEmitterFromUserList() {
        Long userId = 1L;
        SseEmitter emitter1 = new SseEmitter();
        SseEmitter emitter2 = new SseEmitter();
        repository.save(userId, emitter1);
        repository.save(userId, emitter2);

        repository.remove(userId, emitter1);

        assertThat(repository.findAllByUserId(userId)).containsExactly(emitter2);
    }

    @Test
    @DisplayName("마지막 emitter를 제거하면 이후 조회 시 빈 리스트를 반환한다")
    void remove_lastEmitter_leavesEmptyList() {
        Long userId = 1L;
        SseEmitter emitter = new SseEmitter();
        repository.save(userId, emitter);

        repository.remove(userId, emitter);

        assertThat(repository.findAllByUserId(userId)).isEmpty();
    }

    @Test
    @DisplayName("저장된 적 없는 userId를 제거해도 예외가 발생하지 않는다")
    void remove_userIdNeverSaved_doesNotThrow() {
        assertThatCode(() -> repository.remove(999L, new SseEmitter())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 유저의 emitter는 영향받지 않는다")
    void remove_doesNotAffectOtherUsersEmitters() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        SseEmitter emitter1 = new SseEmitter();
        SseEmitter emitter2 = new SseEmitter();
        repository.save(userId1, emitter1);
        repository.save(userId2, emitter2);

        repository.remove(userId1, emitter1);

        assertThat(repository.findAllByUserId(userId2)).containsExactly(emitter2);
    }

    @Test
    @DisplayName("findAll은 등록된 모든 유저의 emitter를 반환한다")
    void findAll_returnsEmittersForEveryUser() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        SseEmitter emitter1 = new SseEmitter();
        SseEmitter emitter2 = new SseEmitter();
        repository.save(userId1, emitter1);
        repository.save(userId2, emitter2);

        Map<Long, List<SseEmitter>> all = repository.findAll();

        assertThat(all).containsOnlyKeys(userId1, userId2);
        assertThat(all.get(userId1)).containsExactly(emitter1);
        assertThat(all.get(userId2)).containsExactly(emitter2);
    }

    @Test
    @DisplayName("등록된 emitter가 없으면 findAll은 빈 맵을 반환한다")
    void findAll_returnsEmptyMap_whenNothingSaved() {
        assertThat(repository.findAll()).isEmpty();
    }
}
