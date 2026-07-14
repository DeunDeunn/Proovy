package com.deundeun.pay.mapper;

import com.deundeun.pay.domain.WithdrawalRequest;
import com.deundeun.pay.dto.WithdrawalItem;
import com.deundeun.pay.enums.WithdrawalStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WithdrawalMapper {
    void insert(WithdrawalRequest withdrawalRequest);

    WithdrawalRequest selectByIdForUpdate(@Param("id") Long id);

    void completeById(@Param("id") Long id, @Param("processedAt") LocalDateTime processedAt);

    void rejectById(@Param("id") Long id,
                     @Param("rejectReason") String rejectReason,
                     @Param("processedAt") LocalDateTime processedAt);

    List<WithdrawalItem> selectByWalletId(@Param("walletId") Long walletId,
                                           @Param("status") WithdrawalStatus status,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    long countByWalletId(@Param("walletId") Long walletId, @Param("status") WithdrawalStatus status);

    List<WithdrawalItem> selectAll(@Param("status") WithdrawalStatus status,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    long countAll(@Param("status") WithdrawalStatus status);
}
