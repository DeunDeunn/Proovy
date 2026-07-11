package com.deundeun.auth.mapper;

import com.deundeun.auth.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByProviderAndProviderId(@Param("provider") String provider,
                                      @Param("providerId") String providerId);

    User findById(@Param("id") Long id);

    void insert(User user);
}
