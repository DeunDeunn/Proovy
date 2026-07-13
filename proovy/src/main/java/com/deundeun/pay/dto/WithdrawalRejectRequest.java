package com.deundeun.pay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRejectRequest {
    @NotBlank(message = "반려 사유를 입력해주세요.")
    @Size(max = 255, message = "반려 사유는 255자 이내로 입력해주세요.")
    private String rejectReason;
}
