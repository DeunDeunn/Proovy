package com.deundeun.auth.mapper;

import com.deundeun.auth.domain.UserVerification;
import com.deundeun.auth.domain.UserVerificationStatus;
import com.deundeun.auth.dto.UserVerificationListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserVerificationMapper {

    void insert(@Param("userId") Long userId);

    boolean existsPending(@Param("userId") Long userId);

    UserVerification findLatestByUserId(@Param("userId") Long userId);

    UserVerification findById(@Param("id") Long id);

    long countSuccessfulChallenges(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    List<UserVerificationListItem> findByStatus(@Param("status") UserVerificationStatus status,
                                                 @Param("limit") int limit, @Param("offset") long offset);

    long countByStatus(@Param("status") UserVerificationStatus status);

    int updateStatus(@Param("id") Long id, @Param("status") UserVerificationStatus status,
                      @Param("approvedAt") LocalDateTime approvedAt, @Param("rejectionReason") String rejectionReason);
}
