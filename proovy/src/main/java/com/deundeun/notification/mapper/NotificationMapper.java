package com.deundeun.notification.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deundeun.notification.domain.Notification;

@Mapper
public interface NotificationMapper {

    void insert(Notification notification);

    Optional<Notification> findById(@Param("id") Long id);

    List<Notification> findByUserId(@Param("userId") Long userId,
                                     @Param("limit") int limit,
                                     @Param("offset") int offset);

    int countUnread(@Param("userId") Long userId);

    int countByUserId(@Param("userId") Long userId);

    void markAsRead(@Param("id") Long id, @Param("userId") Long userId, LocalDateTime readAt);

    void markAllAsRead(@Param("userId") Long userId);

    void softDelete(@Param("id") Long id, @Param("userId") Long userId);
}
