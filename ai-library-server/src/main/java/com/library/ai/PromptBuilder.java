package com.library.ai;

import com.library.dto.BorrowVO;
import com.library.mapper.BorrowRecordMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 提示词组装器：system prompt 模板 + 馆藏数据 + 用户偏好
 */
@Component
public class PromptBuilder {

    /** 系统提示词模板（见设计方案 §7.2），{books_context} 与 {user_profile} 为占位符 */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是"AI 智能书屋"的馆藏智能助手"小书"，任务是帮助用户找书、推荐书、查询借阅相关问题。

            【馆藏数据】（系统检索注入，仅基于以下数据回答）
            {books_context}

            【用户偏好】
            {user_profile}

            【规则】
            1. 推荐书籍时只能从【馆藏数据】中选择，并注明当前是否可借。
            2. 馆藏中没有合适的书时，如实告知，不要编造藏书。
            3. 回答使用简体中文，简洁友好；推荐时给出一句推荐理由。
            4. 与图书/借阅无关的问题（如写代码、闲聊），礼貌引导回图书话题。
            5. 涉及借阅规则：默认借期 30 天，逾期会影响后续借阅。
            """;

    private final BorrowRecordMapper borrowRecordMapper;

    public PromptBuilder(BorrowRecordMapper borrowRecordMapper) {
        this.borrowRecordMapper = borrowRecordMapper;
    }

    /** 组装 system prompt */
    public String buildSystemPrompt(String booksContext, String userProfile) {
        return SYSTEM_PROMPT_TEMPLATE
                .replace("{books_context}", booksContext)
                .replace("{user_profile}", userProfile);
    }

    /** 用户偏好描述：借阅分类分布 + 最近借阅书名，无历史时返回默认文案 */
    public String buildUserProfile(Long userId) {
        List<BorrowVO> recent = borrowRecordMapper.findRecentByUser(userId, 10);
        if (recent.isEmpty()) {
            return "暂无借阅历史";
        }
        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        StringBuilder titles = new StringBuilder();
        for (BorrowVO vo : recent) {
            if (vo.getCategoryName() != null) {
                categoryCount.merge(vo.getCategoryName(), 1, Integer::sum);
            }
            if (vo.getBookTitle() != null) {
                if (!titles.isEmpty()) {
                    titles.append("、");
                }
                titles.append(vo.getBookTitle());
            }
        }
        StringBuilder profile = new StringBuilder("近期借阅分类分布：");
        categoryCount.forEach((name, count) -> profile.append(name).append("×").append(count).append(" "));
        profile.append("；最近借阅：").append(titles);
        return profile.toString();
    }
}
