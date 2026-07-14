package com.deundeun.chat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deundeun.chat.domain.ChatAttachment;

@Mapper
public interface ChatAttachmentMapper {

    void insert(ChatAttachment attachment);

    int linkToMessage(@Param("id") Long id, @Param("messageId") Long messageId, @Param("uploaderId") Long uploaderId);
}
