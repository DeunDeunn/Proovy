package com.deundeun.ai.event;

import java.time.LocalDateTime;

public record AiReviewActivatedEvent(
        Long hostId,
        Long challengeId,
        LocalDateTime activatedAt
) {
}
