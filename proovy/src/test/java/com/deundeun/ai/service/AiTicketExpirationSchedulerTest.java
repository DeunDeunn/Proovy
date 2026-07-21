package com.deundeun.ai.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiTicketExpirationSchedulerTest {

    @Mock
    private AiTicketService aiTicketService;

    @InjectMocks
    private AiTicketExpirationScheduler scheduler;

    @Test
    void expireSubscriptions_delegatesToTicketService() {
        scheduler.expireSubscriptions();

        verify(aiTicketService).expireActiveSubscriptions();
    }
}
