package com.library.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话会话表
 */
@Data
public class ChatSession {

    private Long id;
    private Long userId;
    /** 会话标题（取首条提问前 20 字） */
    private String title;
    private LocalDateTime createTime;
}
