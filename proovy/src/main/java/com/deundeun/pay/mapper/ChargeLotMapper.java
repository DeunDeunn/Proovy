package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.ChargeLot;
import com.deundeun.pay.domain.ChargeLotDeduction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChargeLotMapper {

    void insert(ChargeLot chargeLot);

    /**
     * 잔여액이 남은 lot을 충전일이 오래된 순(FIFO)으로 조회한다.
     * 반드시 지갑에 락(FOR UPDATE)을 먼저 건 트랜잭션 안에서만 호출할 것 —
     * 이 조회 자체는 락을 걸지 않고, 지갑 락에 편승해 안전성을 보장한다.
     */
    List<ChargeLot> selectRemainingByWalletIdOrderByChargedAtAsc(@Param("walletId") Long walletId);

    /**
     * 여러 lot의 remaining_amount를 한 번의 UPDATE로 상대적 차감한다(자바가 미리 읽어둔
     * 절대값을 덮어쓰지 않고, DB의 현재 값 기준으로 계산). lot마다 개별적으로
     * remaining_amount >= deduct 조건을 검사해서, 조건에 걸린 lot은 갱신에서 빠진다.
     * FIFO 루프에서 lot마다 UPDATE를 따로 날리는 대신, 건드릴 lot이 몇 개든 이 한 번의
     * 호출로 끝내서 지갑 락을 잡고 있는 동안의 DB 왕복 횟수를 줄이기 위한 것이다.
     *
     * @return 실제로 갱신된 row 수 (deductions.size()보다 작으면 일부 lot이 조건에 걸려 빠진 것)
     */
    int decrementRemainingAmountBatch(@Param("deductions") List<ChargeLotDeduction> deductions);

    /**
     * 충전일로부터 7일이 지나(withdrawable_at <= now()) 지금 당장 출금 가능한 lot들의 remaining_amount 합.
     */
    long sumWithdrawableRemainingByWalletId(@Param("walletId") Long walletId);

    /**
     * 홀딩 해제(정산 성공 등) 시 charge_lot_allocations에 기록된 만큼 remaining_amount를 되돌릴 때 사용.
     */
    void incrementRemainingAmount(@Param("id") Long id, @Param("amount") long amount);
}
