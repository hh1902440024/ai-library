package com.library.mapper;

import com.library.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 消息表 Mapper（第 4 周实现）
 */
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    /** 按会话查询历史消息（时间正序） */
    List<ChatMessage> findBySessionId(@Param("sessionId") Long sessionId);

    int deleteBySessionId(@Param("sessionId") Long sessionId);
}
