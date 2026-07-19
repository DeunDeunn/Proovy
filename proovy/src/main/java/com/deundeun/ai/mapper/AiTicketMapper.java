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

    int insertSubscription(AiTicketSubscriptionVo subscription);

    int insertTicketHistory(AiTicketHistoryVo history);
}
