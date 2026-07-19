package com.deundeun.ai.mapper;

import com.deundeun.ai.vo.AiTicketPlanVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiTicketMapper {

    List<AiTicketPlanVo> findActivePlans();
}
