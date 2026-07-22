package com.deundeun.ai.event;

import com.deundeun.ai.service.AiReviewService;
import com.deundeun.notification.event.VerificationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class HostCertificationAiReviewListener {

    private final AiReviewService aiReviewService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationSubmittedEvent event) {
        try {
            aiReviewService.reviewHostPost(event.verificationPostId());
        } catch (RuntimeException exception) {
            log.error(
                    "방장 인증글 자동 AI 검수 실패 - verificationPostId={}",
                    event.verificationPostId(),
                    exception
            );
        }
    }
}
