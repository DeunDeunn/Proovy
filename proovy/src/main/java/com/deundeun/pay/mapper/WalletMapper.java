package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WalletMapper {

    Wallet selectByUserId(@Param("userId") Long userId);

    Wallet selectByUserIdForUpdate(@Param("userId") Long userId);

    Wallet selectByIdForUpdate(@Param("id") Long id);

    void insertIfAbsent(@Param("userId") Long userId);

    void updateChargedBalance(@Param("id") Long id, @Param("chargedBalance") long chargedBalance);
}
