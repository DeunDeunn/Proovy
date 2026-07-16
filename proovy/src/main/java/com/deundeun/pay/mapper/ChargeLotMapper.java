package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.ChargeLot;
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
     * remaining_amount에서 deduct만큼 상대적으로 차감한다(자바에서 계산한 절대값을 덮어쓰지 않음).
     * remaining_amount가 deduct보다 작으면(음수가 될 상황) 조건에 걸려 아무 row도 갱신하지 않는다.
     *
     * @return 실제로 갱신된 row 수 (0이면 잔여액 부족 또는 대상 없음)
     */
    int decrementRemainingAmount(@Param("id") Long id, @Param("deduct") long deduct);

    /**
     * 충전일로부터 7일이 지나(withdrawable_at <= now()) 지금 당장 출금 가능한 lot들의 remaining_amount 합.
     */
    long sumWithdrawableRemainingByWalletId(@Param("walletId") Long walletId);

    /**
     * 홀딩 해제(정산 성공 등) 시 charge_lot_allocations에 기록된 만큼 remaining_amount를 되돌릴 때 사용.
     */
    void incrementRemainingAmount(@Param("id") Long id, @Param("amount") long amount);
}
