package com.deundeun.pay.service;

import com.deundeun.pay.dto.SettlementResultResponse;

import java.util.List;

/**
 * room 도메인에서 해당 챌린지 id, 성공/실패 유저 목록, 방장 id, 방장 자격 박탈 여부, 참가비
 * 와 같은 데이터를 매개변수로 넘겨받는다.
 */
public interface WalletSettlementService {
    Long settle(Long challengeId, List<Long> successUserIds, List<Long> failUserIds,
                Long hostId, boolean isHostDisqualified, long perPersonFee);

    /**
     * @throws com.deundeun.global.exception.ApiException SETTLEMENT_NOT_FOUND — 아직 정산되지 않은 챌린지이거나,
     * requesterId가 해당 챌린지의 참가자도 방장도 아닐 때
     */
    SettlementResultResponse getSettlementResult(Long challengeId, Long requesterId);
}
