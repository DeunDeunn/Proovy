package com.deundeun.challenge.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 챌린지 종료 정산 시점에 "이 챌린지에서 방장이 자격을 박탈당했는지"만 조회한다.
 * user_warnings는 인증 도메인이 기록을 남기는 회원 도메인 공용 테이블이라, 쓰기는 그쪽 몫이고
 * 여기서는 정산에 필요한 조회만 가볍게 가져온다.
 */
@Mapper
public interface HostDisqualificationMapper {

    /**
     * 이 챌린지 진행 중 방장이 자동승인(AUTO_APPROVAL) 경고를 받은 적이 있으면 true.
     * 이후 경고가 RESOLVED로 소진됐어도, 이 챌린지에서 실제로 발생한 사실 자체는 변하지 않으므로
     * status와 무관하게 존재 여부만 확인한다.
     */
    boolean existsWarningForChallenge(@Param("challengeId") Long challengeId, @Param("hostId") Long hostId);
}
