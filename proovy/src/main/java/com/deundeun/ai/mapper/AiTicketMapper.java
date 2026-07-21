package com.deundeun.ai.mapper;

import com.deundeun.ai.vo.AiTicketPlanVo;
import com.deundeun.ai.vo.AiTicketHistoryVo;
import com.deundeun.ai.vo.AiTicketSubscriptionVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiTicketMapper {

    List<AiTicketPlanVo> findActivePlans();

    AiTicketPlanVo findPlanById(@Param("planId") Long planId);

    AiTicketSubscriptionVo findActiveSubscriptionByHostId(@Param("hostId") Long hostId);

    AiTicketSubscriptionVo findActiveSubscriptionByHostIdForUpdate(@Param("hostId") Long hostId);

    int insertSubscription(AiTicketSubscriptionVo subscription);

    int insertTicketHistory(AiTicketHistoryVo history);

    int expireActiveSubscriptions();

    List<AiTicketHistoryVo> findTicketHistoriesByHostId(
            @Param("hostId") Long hostId,
            @Param("type") String type,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    long countTicketHistoriesByHostId(@Param("hostId") Long hostId, @Param("type") String type);
}
