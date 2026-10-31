package com.library.mapper;

import com.library.entity.ChatSession;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 会话表 Mapper（第 4 周实现）
 */
public interface ChatSessionMapper {

    int insert(ChatSession session);

    List<ChatSession> findByUserId(@Param("userId") Long userId);

    ChatSession findById(@Param("id") Long id);

    int updateTitle(@Param("id") Long id, @Param("title") String title);

    int deleteById(@Param("id") Long id);
}
