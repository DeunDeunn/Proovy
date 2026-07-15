package com.deundeun.certification.dto;

import lombok.Data;

// 인증글 반려 사유
@Data
public class RejectCertificationPostRequest {
    private String reason;   // 반려 사유
}
