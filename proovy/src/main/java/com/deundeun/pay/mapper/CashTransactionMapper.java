package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.enums.CashTransactionType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CashTransactionMapper {

    void insert(CashTransaction transaction);

    CashTransaction selectById(@Param("id") Long id);

    CashTransaction selectByIdForUpdate(@Param("id") Long id);

    void completeCharge(@Param("id") Long id,
                         @Param("pgTransactionId") String pgTransactionId,
                         @Param("balanceAfter") long balanceAfter);

    int failFromProcessing(@Param("id") Long id);

    int beginProcessing(@Param("id") Long id, @Param("paymentId") String paymentId);

    List<CashTransaction> selectStuckProcessing(@Param("threshold") LocalDateTime threshold);

    List<CashTransaction> selectByWalletId(@Param("walletId") Long walletId,
                                            @Param("type") CashTransactionType type,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    long countByWalletId(@Param("walletId") Long walletId, @Param("type") CashTransactionType type);

    boolean existsSettlementParticipation(@Param("walletId") Long walletId, @Param("referenceId") Long referenceId);

    CashTransaction selectByWalletIdAndReferenceIdAndType(@Param("walletId") Long walletId,
                                                           @Param("referenceId") Long referenceId,
                                                           @Param("type") CashTransactionType type);
}
