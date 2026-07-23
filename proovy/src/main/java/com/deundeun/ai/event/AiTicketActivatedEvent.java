package com.deundeun.ai.event;

import java.time.LocalDateTime;

public record AiTicketActivatedEvent(Long hostId, LocalDateTime activatedAt) {
}
