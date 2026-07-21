package com.deundeun.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiTicketExpirationScheduler {

    private final AiTicketService aiTicketService;

    @Scheduled(fixedDelayString = "${ai.ticket.expiration-check-delay-ms:60000}")
    public void expireSubscriptions() {
        aiTicketService.expireActiveSubscriptions();
    }
}
