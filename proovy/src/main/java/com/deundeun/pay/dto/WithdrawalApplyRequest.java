package com.deundeun.pay.dto;

import com.deundeun.pay.enums.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawalApplyRequest {
      @NotNull(message = "출금 출처를 선택해주세요.")
      private SourceType sourceType;

      @NotNull(message = "출금 금액을 입력해주세요.")
      @Positive(message = "출금 금액은 0보다 커야 합니다.")
      private Long amount;

      @NotBlank(message = "은행명을 입력해주세요.")
      private String bankName;

      @NotBlank(message = "계좌번호를 입력해주세요.")
      private String accountNumber;

      @NotBlank(message = "예금주명을 입력해주세요.")
      private String accountHolderName;
}
