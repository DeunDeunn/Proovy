package com.deundeun.ai.controller;

import com.deundeun.ai.dto.AiTicketPlanResponse;
import com.deundeun.ai.service.AiTicketService;
import com.deundeun.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-tickets")
public class AiTicketController {

    private final AiTicketService aiTicketService;

    @GetMapping("/plans")
    public ApiResponse<List<AiTicketPlanResponse>> findActivePlans() {
        return ApiResponse.success(aiTicketService.findActivePlans());
    }
}
