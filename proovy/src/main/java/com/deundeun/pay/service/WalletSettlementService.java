package com.deundeun.pay.service;

import java.util.List;

/**
 * room 도메인에서 해당 챌린지 id, 성공/실패 유저 목록, 방장 id, 방장 자격 박탈 여부, 참가비
 * 와 같은 데이터를 매개변수로 넘겨받는다.
 */
public interface WalletSettlementService {
    Long settle(Long challengeId, List<Long> successUserIds, List<Long> failUserIds,
                Long hostId, boolean isHostDisqualified, long perPersonFee);
}
