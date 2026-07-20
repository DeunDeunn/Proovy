package com.deundeun.notification.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deundeun.notification.domain.Notification;
import com.deundeun.notification.domain.NotificationType;

@Mapper
public interface NotificationMapper {

    void insert(Notification notification);

    Optional<Notification> findById(@Param("id") Long id);

    List<Notification> findByUserId(@Param("userId") Long userId,
                                     @Param("types") List<NotificationType> types,
                                     @Param("limit") int limit,
                                     @Param("offset") int offset);

    List<Notification> findByUserIdAfterId(@Param("userId") Long userId,
                                            @Param("lastEventId") Long lastEventId,
                                            @Param("limit") int limit);

    int countUnread(@Param("userId") Long userId);

    int countByUserId(@Param("userId") Long userId, @Param("types") List<NotificationType> types);

    void markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    void delete(@Param("id") Long id, @Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);

    int deleteAll(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);
}
