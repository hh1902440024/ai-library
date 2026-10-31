package com.library.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI（LLM）配置项，对应 application.yml 中 ai.* 前缀
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** OpenAI 兼容接口地址（智谱：https://open.bigmodel.cn/api/paas/v4） */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    /** API Key，建议用环境变量 AI_API_KEY 注入 */
    private String apiKey;

    /** 模型名 */
    private String model = "glm-4-flash";

    /** 调用超时（毫秒） */
    private long timeout = 60000;

    /** 对话携带的历史轮数 */
    private int maxHistory = 10;

    /** 注入提示词的馆藏条数上限 */
    private int maxContextBooks = 20;
}
