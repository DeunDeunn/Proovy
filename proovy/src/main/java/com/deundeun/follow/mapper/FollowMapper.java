package com.deundeun.follow.mapper;

import com.deundeun.follow.dto.FollowUserItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FollowMapper {

    void insert(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    void delete(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    boolean exists(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    List<FollowUserItem> findFollowers(@Param("followingId") Long followingId,
                                        @Param("limit") int limit, @Param("offset") int offset);

    long countFollowers(@Param("followingId") Long followingId);

    List<FollowUserItem> findFollowing(@Param("followerId") Long followerId,
                                        @Param("limit") int limit, @Param("offset") int offset);

    long countFollowing(@Param("followerId") Long followerId);
}
