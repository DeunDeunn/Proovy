package com.deundeun.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deundeun.notification.domain.Notification;

@Mapper
public interface NotificationMapper {

    void insert(Notification notification);

    List<Notification> findByUserId(@Param("userId") Long userId,
                                     @Param("limit") int limit,
                                     @Param("offset") int offset);

    int countUnread(@Param("userId") Long userId);

    void markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    void markAllAsRead(@Param("userId") Long userId);

    void softDelete(@Param("id") Long id, @Param("userId") Long userId);
}
