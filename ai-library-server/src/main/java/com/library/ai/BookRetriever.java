package com.library.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.entity.Book;
import com.library.mapper.BookMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 馆藏检索器（简化 RAG）：
 * 1. 从用户问题提取检索词（LLM 轻量抽取，失败退化为原句直接模糊匹配）
 * 2. MySQL 检索 title/author/tags/description，按匹配度与可借库存排序，取 Top N
 */
@Slf4j
@Component
public class BookRetriever {

    private static final String EXTRACT_PROMPT = """
            从用户问题中提取用于图书检索的关键词（书名/作者/分类/主题词），最多 3 个。
            只输出 JSON，格式：{"keywords": ["关键词1", "关键词2"]}，不要输出其他内容。
            用户问题：%s
            """;

    private final LlmClient llmClient;
    private final BookMapper bookMapper;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public BookRetriever(LlmClient llmClient, BookMapper bookMapper, AiProperties aiProperties, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.bookMapper = bookMapper;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 检索与问题相关的馆藏，返回最多 maxContextBooks 条（去重保序）
     */
    public List<Book> retrieve(String question) {
        int limit = aiProperties.getMaxContextBooks();
        Map<Long, Book> merged = new LinkedHashMap<>();

        List<String> keywords = extractKeywords(question);
        for (String keyword : keywords) {
            for (Book book : bookMapper.searchForRag(keyword, null, limit)) {
                merged.putIfAbsent(book.getId(), book);
                if (merged.size() >= limit) {
                    return new ArrayList<>(merged.values());
                }
            }
        }
        // 关键词抽取失败或无命中时，退化为原句模糊匹配
        if (merged.isEmpty()) {
            for (Book book : bookMapper.searchForRag(question, null, limit)) {
                merged.putIfAbsent(book.getId(), book);
                if (merged.size() >= limit) {
                    break;
                }
            }
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * 将图书列表组装为提示词上下文：
     * 编号.《书名》| 作者 | 分类 | 可借 x/总 y | 标签 | 简介(截断200字)
     */
    public String buildBooksContext(List<Book> books) {
        if (books.isEmpty()) {
            return "（当前未检索到相关馆藏）";
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Book book : books) {
            String desc = book.getDescription() == null ? "" : book.getDescription();
            if (desc.length() > 200) {
                desc = desc.substring(0, 200) + "…略";
            }
            sb.append(i++).append(".《").append(book.getTitle()).append("》")
                    .append("| ").append(book.getAuthor())
                    .append("| 可借 ").append(book.getAvailableStock()).append("/总 ").append(book.getTotalStock())
                    .append("| ").append(book.getTags() == null ? "" : book.getTags())
                    .append("| ").append(desc)
                    .append("\n");
        }
        return sb.toString();
    }

    /** LLM 轻量关键词抽取，失败返回仅含原句的列表 */
    private List<String> extractKeywords(String question) {
        try {
            String answer = llmClient.chatSync(List.of(
                    LlmClient.message("system", "你是一个关键词提取器，只输出 JSON。"),
                    LlmClient.message("user", EXTRACT_PROMPT.formatted(question))
            ));
            // 兼容模型输出被 ```json 包裹的情况
            String json = answer.replaceAll("```json|```", "").trim();
            JsonNode node = objectMapper.readTree(json);
            List<String> keywords = new ArrayList<>();
            node.path("keywords").forEach(k -> {
                String kw = k.asText("").trim();
                if (!kw.isEmpty()) {
                    keywords.add(kw);
                }
            });
            if (!keywords.isEmpty()) {
                return keywords;
            }
        } catch (Exception e) {
            log.warn("关键词抽取失败，退化为原句匹配: {}", e.getMessage());
        }
        return List.of(question);
    }
}
