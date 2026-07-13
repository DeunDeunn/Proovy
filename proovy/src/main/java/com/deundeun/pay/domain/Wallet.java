package com.deundeun.pay.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {
    private Long id;
    private Long userId;
    private Long chargedBalance;
    private Long rewardBalance;
    private Long lockedChargedBalance;
    private Long lockedRewardBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long getLockedBalance() {
        return lockedChargedBalance + lockedRewardBalance;
    }

    public long getAvailableBalance() {
        return chargedBalance + rewardBalance - getLockedBalance();
    }

    public long getUnlockedRewardBalance() {
        return rewardBalance - lockedRewardBalance;
    }
}
