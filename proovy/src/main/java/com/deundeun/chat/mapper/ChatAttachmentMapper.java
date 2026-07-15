package com.deundeun.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deundeun.chat.domain.ChatAttachment;

@Mapper
public interface ChatAttachmentMapper {

    void insert(ChatAttachment attachment);

    int linkToMessage(@Param("id") Long id, @Param("messageId") Long messageId, @Param("uploaderId") Long uploaderId);

    List<ChatAttachment> findByMessageIds(@Param("messageIds") List<Long> messageIds);
}
