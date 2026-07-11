package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.ChargeLot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChargeLotMapper {

    void insert(ChargeLot chargeLot);
}
