package com.deundeun.ai.event;

import com.deundeun.ai.mapper.AiReviewMapper;
import com.deundeun.ai.service.AiReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AI 검수 활성화 후 기존 대기 인증글 검수 이벤트 리스너")
@ExtendWith(MockitoExtension.class)
class AiReviewActivatedListenerTest {

    @Mock
    private AiReviewMapper aiReviewMapper;

    @Mock
    private AiReviewService aiReviewService;

    @InjectMocks
    private AiReviewActivatedListener listener;

    @Test
    @DisplayName("활성화 전 대기 참가자 인증글을 오래된 순서대로 검수한다")
    void handle_reviewsPendingParticipantPostsInOrder() {
        LocalDateTime activatedAt = LocalDateTime.of(2026, 7, 23, 12, 0);
        when(aiReviewMapper.findPendingParticipantPostIdsBeforeAiReviewActivation(10L, activatedAt))
                .thenReturn(List.of(20L, 21L));

        listener.handle(new AiReviewActivatedEvent(1L, 10L, activatedAt));

        InOrder inOrder = inOrder(aiReviewService);
        inOrder.verify(aiReviewService).reviewSubmittedPost(20L);
        inOrder.verify(aiReviewService).reviewSubmittedPost(21L);
    }

    @Test
    @DisplayName("한 게시물 검수 실패가 다음 대기 게시물 검수를 막지 않는다")
    void handle_continuesAfterReviewFailure() {
        LocalDateTime activatedAt = LocalDateTime.of(2026, 7, 23, 12, 0);
        when(aiReviewMapper.findPendingParticipantPostIdsBeforeAiReviewActivation(10L, activatedAt))
                .thenReturn(List.of(20L, 21L));
        doThrow(new IllegalStateException("AI failure"))
                .when(aiReviewService).reviewSubmittedPost(20L);

        assertThatCode(() -> listener.handle(new AiReviewActivatedEvent(1L, 10L, activatedAt)))
                .doesNotThrowAnyException();

        verify(aiReviewService).reviewSubmittedPost(21L);
    }
}
