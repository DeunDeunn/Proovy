package com.deundeun.certification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoApprovalScheduler - 자정 트리거")
class AutoApprovalSchedulerTest {

    @Mock
    private AutoApprovalService autoApprovalService;

    @InjectMocks
    private AutoApprovalScheduler autoApprovalScheduler;

    @Test
    @DisplayName("[S-01] 자동승인 서비스에 위임한다")
    void delegatesToService() {
        autoApprovalScheduler.runMidnightAutoApproval();

        verify(autoApprovalService).autoApproveAllPending();
    }

    @Test
    @DisplayName("[S-02] 서비스에서 예외가 나도 스케줄러 밖으로 전파하지 않는다")
    void swallowsException() {
        doThrow(new RuntimeException("boom")).when(autoApprovalService).autoApproveAllPending();

        // 다음 날 자정에 다시 도므로 예외를 삼켜야 한다
        assertThatCode(() -> autoApprovalScheduler.runMidnightAutoApproval())
                .doesNotThrowAnyException();
    }
}
