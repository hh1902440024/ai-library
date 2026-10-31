package com.library.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话消息表
 */
@Data
public class ChatMessage {

    private Long id;
    private Long sessionId;
    /** user / assistant */
    private String role;
    private String content;
    private LocalDateTime createTime;
}
