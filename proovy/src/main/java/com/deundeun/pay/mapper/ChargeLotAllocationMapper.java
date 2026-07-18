package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.ChargeLotAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChargeLotAllocationMapper {

    void insert(ChargeLotAllocation allocation);

    /**
     * 여러 lot에 대한 할당 기록을 한 번의 INSERT로 남긴다. FIFO 루프에서 건드린 lot이
     * 몇 개든, 그만큼 insert()를 반복 호출하는 대신 한 번의 왕복으로 끝내기 위한 것이다.
     */
    void insertAll(@Param("allocations") List<ChargeLotAllocation> allocations);

    List<ChargeLotAllocation> selectByWalletIdAndReferenceId(@Param("walletId") Long walletId,
                                                               @Param("referenceId") Long referenceId);

    /**
     * 정산 성공 복구(releaseChargeLotsFifo) 전용. 아직 복구 처리 안 된(released_at IS NULL) 것만 조회해서,
     * successUserIds에 같은 유저가 중복으로 들어와도 같은 홀딩이 두 번 복구되지 않게 한다.
     */
    List<ChargeLotAllocation> selectUnreleasedByWalletIdAndReferenceId(@Param("walletId") Long walletId,
                                                                        @Param("referenceId") Long referenceId);

    void markReleased(@Param("id") Long id, @Param("releasedAt") LocalDateTime releasedAt);
}
