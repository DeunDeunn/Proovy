package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.Settlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettlementMapper {
    boolean existsByChallengeId(@Param("challengeId") Long challengeId);
    void insert(Settlement settlement);
}
