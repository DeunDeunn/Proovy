package com.deundeun.certification.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CertificationAiReviewResponse {
    private Long id;
    private String status;
    private String reviewMode;
    private String decision;
    private String reason;
    private String criteria;
    private LocalDateTime requestedAt;
}
