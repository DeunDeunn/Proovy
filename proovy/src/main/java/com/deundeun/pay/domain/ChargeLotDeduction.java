package com.deundeun.pay.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FIFO 차감 루프에서 "이 lot에서 이만큼 뺀다"를 계산해둔 결과를, 여러 lot을 한 번의
 * 배치 UPDATE로 처리하기 위해 담는 파라미터 객체. 순수 DB 왕복 횟수를 줄이는 용도라
 * charge_lot_allocations처럼 별도 기록이 필요 없는 deductChargeLotsFifo에서도 그대로 쓴다.
 */
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeLotDeduction {
    private Long chargeLotId;
    private Long deduct;
}
