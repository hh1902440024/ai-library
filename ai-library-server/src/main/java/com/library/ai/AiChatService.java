package com.library.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.ResultCode;
import com.library.dto.BorrowVO;
import com.library.dto.SummaryVO;
import com.library.entity.Book;
import com.library.entity.ChatMessage;
import com.library.entity.ChatSession;
import com.library.exception.BizException;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.ChatMessageMapper;
import com.library.mapper.ChatSessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 对话服务：会话管理、历史裁剪、检索增强、流式输出、落库与降级
 *
 * <p>对话流程（见设计方案 §10.3）：
 * ① sessionId 为空则新建会话（标题=首问前20字） → ② 用户消息落库 →
 * ③ BookRetriever 检索馆藏 + 用户偏好统计 → ④ PromptBuilder 组装上下文 →
 * ⑤ LlmClient 流式调用 → 逐段 emitter.send() → ⑥ 完整回答落库 → send("[DONE]") → complete()
 */
@Slf4j
@Service
public class AiChatService {

    /** SSE 超时 120s */
    private static final long SSE_TIMEOUT = 120_000L;
    private static final String FALLBACK_TEXT = "AI 助手暂时开小差了，请稍后再试";

    /** LLM 调用线程池（守护线程，不阻塞 Tomcat 请求线程） */
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ai-chat-" + r.hashCode());
        t.setDaemon(true);
        return t;
    });

    private final LlmClient llmClient;
    private final BookRetriever bookRetriever;
    private final PromptBuilder promptBuilder;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BookMapper bookMapper;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public AiChatService(LlmClient llmClient, BookRetriever bookRetriever, PromptBuilder promptBuilder,
                         ChatSessionMapper chatSessionMapper, ChatMessageMapper chatMessageMapper,
                         BorrowRecordMapper borrowRecordMapper, BookMapper bookMapper,
                         AiProperties aiProperties, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.bookRetriever = bookRetriever;
        this.promptBuilder = promptBuilder;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.borrowRecordMapper = borrowRecordMapper;
        this.bookMapper = bookMapper;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * SSE 流式对话。事件协议：
     * event=session data={"sessionId":1} → 默认事件 data={"delta":"..."} → data=[DONE]
     */
    public SseEmitter chat(Long userId, Long sessionId, String content) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        // sessionId 在任务线程中可能被赋新值（新建会话），用 AtomicReference 持有
        java.util.concurrent.atomic.AtomicReference<Long> sidRef = new java.util.concurrent.atomic.AtomicReference<>(sessionId);

        executor.execute(() -> {
            try {
                // ① 会话：为空则新建（标题取首问前 20 字）
                if (sidRef.get() == null) {
                    ChatSession session = new ChatSession();
                    session.setUserId(userId);
                    session.setTitle(content.length() > 20 ? content.substring(0, 20) : content);
                    chatSessionMapper.insert(session);
                    sidRef.set(session.getId());
                } else {
                    checkSessionOwner(userId, sidRef.get());
                }
                Long sid = sidRef.get();
                send(emitter, "session", objectMapper.writeValueAsString(Map.of("sessionId", sid)));

                // ② 用户消息落库
                ChatMessage userMessage = new ChatMessage();
                userMessage.setSessionId(sid);
                userMessage.setRole("user");
                userMessage.setContent(content);
                chatMessageMapper.insert(userMessage);

                // ③④ 检索馆藏 + 用户偏好 → system prompt
                List<ChatMessage> history = chatMessageMapper.findBySessionId(sid);
                String systemPrompt = promptBuilder.buildSystemPrompt(
                        bookRetriever.buildBooksContext(bookRetriever.retrieve(content)),
                        promptBuilder.buildUserProfile(userId));

                // ⑤ 组装消息（system + 最近 N 轮历史，含本轮提问）
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(LlmClient.message("system", systemPrompt));
                int from = Math.max(0, history.size() - aiProperties.getMaxHistory() * 2);
                for (ChatMessage msg : history.subList(from, history.size())) {
                    messages.add(LlmClient.message(msg.getRole(), msg.getContent()));
                }

                // 流式调用，逐段推送；结束后完整回答落库
                StringBuilder full = new StringBuilder();
                llmClient.chatStream(messages, delta -> {
                    full.append(delta);
                    try {
                        send(emitter, null, objectMapper.writeValueAsString(Map.of("delta", delta)));
                    } catch (Exception e) {
                        log.warn("SSE 推送失败（客户端可能已断开）: {}", e.getMessage());
                    }
                });

                // ⑥ 完整回答落库 + [DONE] + complete
                ChatMessage assistantMessage = new ChatMessage();
                assistantMessage.setSessionId(sid);
                assistantMessage.setRole("assistant");
                assistantMessage.setContent(full.toString());
                chatMessageMapper.insert(assistantMessage);

                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
                log.info("AI 对话完成: userId={}, sessionId={}, 回答长度={}", userId, sid, full.length());
            } catch (Exception e) {
                log.error("AI 对话异常: userId={}, sessionId={}", userId, sidRef.get(), e);
                fallback(emitter);
            }
        });
        return emitter;
    }

    /** 个性化推荐：有历史 → 基于偏好推荐 3~5 本；无历史 → 全馆热门 Top5 */
    public String recommend(Long userId) {
        List<BorrowVO> recent = borrowRecordMapper.findRecentByUser(userId, 10);
        List<Book> candidates;
        String profile;

        if (recent.isEmpty()) {
            candidates = bookMapper.searchForRag(null, null, 20);
            profile = "该用户暂无借阅历史，请从热门馆藏中推荐。";
        } else {
            profile = promptBuilder.buildUserProfile(userId);
            // 用最近借阅的分类/书名作为检索词找候选（排除已借阅过的书）
            Map<Long, Book> merged = new LinkedHashMap<>();
            recent.stream().map(BorrowVO::getCategoryName).distinct().limit(3)
                    .forEach(cat -> bookMapper.searchForRag(cat, null, 20).forEach(b -> merged.putIfAbsent(b.getId(), b)));
            merged.values().removeIf(b -> recent.stream().anyMatch(r -> r.getBookId().equals(b.getId())));
            candidates = new ArrayList<>(merged.values());
            if (candidates.isEmpty()) {
                candidates = bookMapper.searchForRag(null, null, 20);
            }
        }

        String prompt = """
                请根据用户偏好，从下方候选馆藏中挑选 3~5 本书推荐给用户。
                每本格式：**《书名》**（可借 x/总 y）—— 一句话推荐理由。
                用户偏好：%s
                候选馆藏：
                %s
                """.formatted(profile, bookRetriever.buildBooksContext(candidates));

        try {
            return llmClient.chatSync(List.of(
                    LlmClient.message("system", "你是\"AI 智能书屋\"的推荐助手，基于馆藏数据生成推荐书单，简体中文回答。"),
                    LlmClient.message("user", prompt)
            ));
        } catch (Exception e) {
            log.error("个性化推荐失败: userId={}", userId, e);
            throw new BizException(ResultCode.SERVER_ERROR, FALLBACK_TEXT);
        }
    }

    /** 图书自动摘要：LLM 返回 JSON {description, tags[]}，解析失败重试一次 */
    public SummaryVO generateSummary(Long bookId, String points) {
        Book book = bookMapper.findById(bookId);
        if (book == null) {
            throw new BizException(ResultCode.NOT_FOUND, "图书不存在");
        }
        String prompt = """
                请为以下图书生成一段 100~200 字的中文简介和 3~5 个标签。
                书名：%s
                作者：%s
                出版社：%s
                目录/要点：%s
                只输出 JSON，格式：{"description": "简介内容", "tags": ["标签1", "标签2"]}，不要输出其他内容。
                """.formatted(book.getTitle(), book.getAuthor(),
                book.getPublisher() == null ? "未知" : book.getPublisher(),
                points == null || points.isBlank() ? "无（请根据书名与作者合理概括）" : points);

        String system = "你是图书信息编辑，只输出 JSON。";
        // 解析失败重试一次（设计方案 §7.6 容错）
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String answer = llmClient.chatSync(List.of(LlmClient.message("system", system), LlmClient.message("user", prompt)));
                String json = answer.replaceAll("```json|```", "").trim();
                JsonNode node = objectMapper.readTree(json);
                String description = node.path("description").asText("");
                StringBuilder tags = new StringBuilder();
                node.path("tags").forEach(t -> {
                    if (tags.length() > 0) {
                        tags.append(",");
                    }
                    tags.append(t.asText("").trim());
                });
                if (!description.isEmpty() && !tags.isEmpty()) {
                    return new SummaryVO(description, tags.toString());
                }
            } catch (Exception e) {
                log.warn("AI 摘要解析失败（第 {} 次）: {}", attempt, e.getMessage());
            }
        }
        throw new BizException("AI 生成摘要失败，请手动填写");
    }

    /** 我的会话列表 */
    public List<ChatSession> mySessions(Long userId) {
        return chatSessionMapper.findByUserId(userId);
    }

    /** 会话历史消息（校验本人） */
    public List<ChatMessage> messages(Long userId, Long sessionId) {
        checkSessionOwner(userId, sessionId);
        return chatMessageMapper.findBySessionId(sessionId);
    }

    /** 删除会话及其消息（校验本人） */
    public void deleteSession(Long userId, Long sessionId) {
        checkSessionOwner(userId, sessionId);
        chatMessageMapper.deleteBySessionId(sessionId);
        chatSessionMapper.deleteById(sessionId);
    }

    private void checkSessionOwner(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.findById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
        }
    }

    private void send(SseEmitter emitter, String eventName, String data) {
        try {
            if (eventName == null) {
                emitter.send(SseEmitter.event().data(data));
            } else {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 降级：发送兜底文案后正常结束，前端体验不中断 */
    private void fallback(SseEmitter emitter) {
        try {
            send(emitter, null, objectMapper.writeValueAsString(Map.of("delta", FALLBACK_TEXT)));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
