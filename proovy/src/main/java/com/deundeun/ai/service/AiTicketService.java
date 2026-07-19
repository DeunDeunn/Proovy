package com.deundeun.ai.service;

import com.deundeun.ai.dto.AiTicketPlanResponse;
import com.deundeun.ai.mapper.AiTicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiTicketService {

    private final AiTicketMapper aiTicketMapper;

    @Transactional(readOnly = true)
    public List<AiTicketPlanResponse> findActivePlans() {
        return aiTicketMapper.findActivePlans().stream()
                .map(AiTicketPlanResponse::from)
                .toList();
    }
}
