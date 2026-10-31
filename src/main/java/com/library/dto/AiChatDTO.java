package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 对话请求
 */
@Data
public class AiChatDTO {

    /** 为空则新建会话 */
    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息长度不能超过 2000 字")
    private String content;
}
