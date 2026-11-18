package com.library.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.ResultCode;
import com.library.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM 客户端：封装 GLM（OpenAI 兼容 Chat Completions）同步与 SSE 流式调用
 * 更换 DeepSeek/通义千问等只需改 base-url 与 api-key
 */
@Slf4j
@Component
public class LlmClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LlmClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getTimeout()))
                .build();
    }

    /** 单条消息便捷封装 */
    public static Map<String, String> message(String role, String content) {
        return Map.of("role", role, "content", content);
    }

    /**
     * 同步调用：一次性返回完整回答（用于个性化推荐、关键词抽取、图书摘要）
     */
    public String chatSync(List<Map<String, String>> messages) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> body = Map.of(
                    "model", properties.getModel(),
                    "messages", messages,
                    "stream", false
            );
            HttpRequest request = buildRequest(body);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("LLM 同步调用失败: status={}, body={}", response.statusCode(), abbreviate(response.body()));
                throw new BizException(ResultCode.SERVER_ERROR, "AI 服务响应异常");
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            JsonNode usage = root.path("usage");
            log.info("LLM 同步调用完成: 耗时 {}ms, promptTokens={}, completionTokens={}",
                    System.currentTimeMillis() - start,
                    usage.path("prompt_tokens").asLong(-1), usage.path("completion_tokens").asLong(-1));
            return content;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM 同步调用异常", e);
            throw new BizException(ResultCode.SERVER_ERROR, "AI 助手暂时开小差了，请稍后再试");
        }
    }

    /**
     * 流式调用：逐段回调增量内容（用于 AI 问答打字机效果），返回完整回答
     */
    public String chatStream(List<Map<String, String>> messages, Consumer<String> onDelta) {
        long start = System.currentTimeMillis();
        StringBuilder full = new StringBuilder();
        try {
            Map<String, Object> body = Map.of(
                    "model", properties.getModel(),
                    "messages", messages,
                    "stream", true
            );
            HttpRequest request = buildRequest(body);
            // ofLines：响应头到达后 send 返回，行数据可边收边处理
            HttpResponse<java.util.stream.Stream<String>> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() != 200) {
                log.error("LLM 流式调用失败: status={}", response.statusCode());
                throw new BizException(ResultCode.SERVER_ERROR, "AI 服务响应异常");
            }
            try (java.util.stream.Stream<String> lines = response.body()) {
                for (java.util.Iterator<String> it = lines.iterator(); it.hasNext(); ) {
                    String line = it.next();
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if ("[DONE]".equals(payload)) {
                        break;
                    }
                    try {
                        JsonNode node = objectMapper.readTree(payload);
                        String delta = node.path("choices").path(0).path("delta").path("content").asText("");
                        if (!delta.isEmpty()) {
                            full.append(delta);
                            onDelta.accept(delta);
                        }
                    } catch (Exception parseError) {
                        log.warn("跳过无法解析的 SSE 片段: {}", abbreviate(payload));
                    }
                }
            }
            log.info("LLM 流式调用完成: 耗时 {}ms, 回答长度 {}", System.currentTimeMillis() - start, full.length());
            return full.toString();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM 流式调用异常", e);
            // 已输出的增量保留，异常向上抛出由调用方决定是否降级
            throw new BizException(ResultCode.SERVER_ERROR, "AI 助手暂时开小差了，请稍后再试");
        }
    }

    private HttpRequest buildRequest(Map<String, Object> body) throws Exception {
        String url = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
    }

    private String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
