package com.deundeun.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.deundeun.ai.dto.AiTicketPlanResponse;
import com.deundeun.ai.mapper.AiTicketMapper;
import com.deundeun.ai.vo.AiTicketPlanVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@DisplayName("AiTicketService")
@ExtendWith(MockitoExtension.class)
class AiTicketServiceTest {

    @Mock
    private AiTicketMapper aiTicketMapper;

    @InjectMocks
    private AiTicketService aiTicketService;

    @Test
    @DisplayName("활성 AI 티켓 상품 목록을 응답 DTO로 변환한다")
    void findActivePlans_returnsResponses() {
        AiTicketPlanVo oneDayPlan = AiTicketPlanVo.builder()
                .id(1L)
                .name("1일 AI 티켓")
                .durationDays(1)
                .price(1000)
                .description("24시간 동안 AI 검수 기능 사용 가능")
                .active(true)
                .build();

        when(aiTicketMapper.findActivePlans()).thenReturn(List.of(oneDayPlan));

        List<AiTicketPlanResponse> responses = aiTicketService.findActivePlans();

        assertThat(responses).hasSize(1);
        AiTicketPlanResponse response = responses.getFirst();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("1일 AI 티켓");
        assertThat(response.getDurationDays()).isEqualTo(1);
        assertThat(response.getPrice()).isEqualTo(1000);
        assertThat(response.getDescription()).isEqualTo("24시간 동안 AI 검수 기능 사용 가능");
        assertThat(response.getActive()).isTrue();
    }
}
