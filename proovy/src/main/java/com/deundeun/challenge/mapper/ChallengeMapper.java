package com.deundeun.challenge.mapper;

import java.util.List;

import com.deundeun.challenge.domain.Challenge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChallengeMapper {
    void insert(Challenge challenge);

    List<Challenge> findByIds(@Param("ids") List<Long> ids);
}
