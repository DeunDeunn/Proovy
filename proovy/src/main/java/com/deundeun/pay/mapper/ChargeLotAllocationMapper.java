package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.ChargeLotAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChargeLotAllocationMapper {

    void insert(ChargeLotAllocation allocation);

    List<ChargeLotAllocation> selectByWalletIdAndReferenceId(@Param("walletId") Long walletId,
                                                               @Param("referenceId") Long referenceId);
}
