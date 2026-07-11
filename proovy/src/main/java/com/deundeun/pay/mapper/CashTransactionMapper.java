package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.domain.CashTransactionStatus;
import com.deundeun.pay.domain.CashTransactionType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CashTransactionMapper {

    void insert(CashTransaction transaction);

    CashTransaction selectById(@Param("id") Long id);

    void completeCharge(@Param("id") Long id,
                         @Param("pgTransactionId") String pgTransactionId,
                         @Param("balanceAfter") long balanceAfter);

    void updateStatus(@Param("id") Long id, @Param("status") CashTransactionStatus status);

    List<CashTransaction> selectByWalletId(@Param("walletId") Long walletId,
                                            @Param("type") CashTransactionType type,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    long countByWalletId(@Param("walletId") Long walletId, @Param("type") CashTransactionType type);
}
