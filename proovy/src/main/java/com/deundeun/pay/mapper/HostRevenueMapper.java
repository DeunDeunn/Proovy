package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.HostRevenue;
import com.deundeun.pay.dto.HostRevenueItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HostRevenueMapper {
    void insert(HostRevenue hostRevenue);

    HostRevenueItem selectByChallengeId(@Param("challengeId") Long challengeId);

    List<HostRevenueItem> selectByHostId(@Param("hostId") Long hostId,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    long countByHostId(@Param("hostId") Long hostId);

    boolean existsByChallengeIdAndHostId(@Param("challengeId") Long challengeId, @Param("hostId") Long hostId);
}
