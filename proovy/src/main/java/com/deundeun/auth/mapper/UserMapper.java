package com.deundeun.auth.mapper;

import java.util.List;

import com.deundeun.auth.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByProviderAndProviderId(@Param("provider") String provider,
                                      @Param("providerId") String providerId);

    User findById(@Param("id") Long id);

    List<User> findByIds(@Param("ids") List<Long> ids);

    void insert(User user);

    void softDelete(@Param("id") Long id);

    boolean existsActiveChallengeParticipation(@Param("userId") Long userId);

    boolean existsByNickname(@Param("nickname") String nickname, @Param("excludeUserId") Long excludeUserId);

    void updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
}
