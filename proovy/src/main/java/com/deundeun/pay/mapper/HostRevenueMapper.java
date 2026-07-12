package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.HostRevenue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HostRevenueMapper {
    void insert(HostRevenue hostRevenue);
}
