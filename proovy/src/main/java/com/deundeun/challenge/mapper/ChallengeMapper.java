package com.deundeun.challenge.mapper;

import com.deundeun.challenge.domain.Challenge;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChallengeMapper {
    void insert(Challenge challenge);
}
