package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.Settlement;
import com.deundeun.pay.dto.SettlementHistoryItem;
import com.deundeun.pay.dto.SettlementResultResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettlementMapper {
    boolean existsByChallengeId(@Param("challengeId") Long challengeId);
    void insert(Settlement settlement);
    SettlementResultResponse selectByChallengeId(@Param("challengeId") Long challengeId);

    List<SettlementHistoryItem> selectMyHistory(@Param("userId") Long userId,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    long countMyHistory(@Param("userId") Long userId);
}
