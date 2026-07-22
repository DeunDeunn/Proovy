package com.deundeun.ai.event;

import com.deundeun.ai.mapper.AiReviewMapper;
import com.deundeun.ai.service.AiReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketActivatedAiReviewListener {

    private final AiReviewMapper aiReviewMapper;
    private final AiReviewService aiReviewService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AiTicketActivatedEvent event) {
        for (Long postId : aiReviewMapper.findPendingParticipantPostIdsByHostId(event.hostId())) {
            try {
                aiReviewService.reviewSubmittedPost(postId);
            } catch (RuntimeException exception) {
                log.error(
                        "티켓 활성화 후 대기 인증글 AI 검수 실패 - hostId={}, verificationPostId={}",
                        event.hostId(),
                        postId,
                        exception
                );
            }
        }
    }
}
