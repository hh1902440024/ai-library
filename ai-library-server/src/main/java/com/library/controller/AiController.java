package com.library.controller;

import com.library.ai.AiChatService;
import com.library.common.Result;
import com.library.dto.AiChatDTO;
import com.library.entity.ChatMessage;
import com.library.entity.ChatSession;
import com.library.interceptor.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 智能体接口
 */
@Tag(name = "AI 智能体")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;

    @Operation(summary = "AI 对话（SSE 流式返回）")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatDTO dto) {
        return aiChatService.chat(UserContext.getUserId(), dto.getSessionId(), dto.getContent());
    }

    @Operation(summary = "我的会话列表")
    @GetMapping("/sessions")
    public Result<List<ChatSession>> sessions() {
        return Result.ok(aiChatService.mySessions(UserContext.getUserId()));
    }

    @Operation(summary = "会话历史消息")
    @GetMapping("/sessions/{id}/messages")
    public Result<List<ChatMessage>> messages(@PathVariable Long id) {
        return Result.ok(aiChatService.messages(UserContext.getUserId(), id));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        aiChatService.deleteSession(UserContext.getUserId(), id);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "个性化推荐（基于借阅历史）")
    @GetMapping("/recommend")
    public Result<String> recommend() {
        return Result.ok(aiChatService.recommend(UserContext.getUserId()));
    }
}
