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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("티켓 활성화 후 대기 인증글 자동 AI 검수 이벤트 리스너")
@ExtendWith(MockitoExtension.class)
class TicketActivatedAiReviewListenerTest {

    @Mock
    private AiReviewMapper aiReviewMapper;

    @Mock
    private AiReviewService aiReviewService;

    @InjectMocks
    private TicketActivatedAiReviewListener listener;

    @Test
    @DisplayName("기존 대기 참가자 인증글을 오래된 순서대로 자동 검수한다")
    void handle_reviewsPendingParticipantPostsInOrder() {
        when(aiReviewMapper.findPendingParticipantPostIdsByHostId(1L))
                .thenReturn(List.of(10L, 11L));

        listener.handle(new AiTicketActivatedEvent(1L));

        InOrder inOrder = inOrder(aiReviewService);
        inOrder.verify(aiReviewService).reviewSubmittedPost(10L);
        inOrder.verify(aiReviewService).reviewSubmittedPost(11L);
    }

    @Test
    @DisplayName("한 게시물 검수 실패가 다음 대기 게시물 검수를 막지 않는다")
    void handle_continuesAfterReviewFailure() {
        when(aiReviewMapper.findPendingParticipantPostIdsByHostId(1L))
                .thenReturn(List.of(10L, 11L));
        doThrow(new IllegalStateException("AI failure"))
                .when(aiReviewService).reviewSubmittedPost(10L);

        assertThatCode(() -> listener.handle(new AiTicketActivatedEvent(1L)))
                .doesNotThrowAnyException();

        verify(aiReviewService).reviewSubmittedPost(11L);
    }
}
