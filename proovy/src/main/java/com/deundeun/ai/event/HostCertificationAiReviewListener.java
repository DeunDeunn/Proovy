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

    private static final int MAX_REVIEW_ATTEMPTS = 3;

    private final AiReviewService aiReviewService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationSubmittedEvent event) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_REVIEW_ATTEMPTS; attempt++) {
            try {
                aiReviewService.reviewSubmittedPost(event.verificationPostId());
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                log.warn(
                        "인증글 자동 AI 검수 실패 - verificationPostId={}, attempt={}/{}",
                        event.verificationPostId(),
                        attempt,
                        MAX_REVIEW_ATTEMPTS,
                        exception
                );
            }
        }

        try {
            aiReviewService.rejectHostPostAfterReviewFailure(event.verificationPostId());
        } catch (RuntimeException fallbackException) {
            if (lastFailure != null) {
                fallbackException.addSuppressed(lastFailure);
            }
            log.error(
                    "AI 검수 최종 실패 후 방장 인증글 자동 반려 실패 - verificationPostId={}",
                    event.verificationPostId(),
                    fallbackException
            );
        }
    }
}
