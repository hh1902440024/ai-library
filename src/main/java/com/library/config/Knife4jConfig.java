package com.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（Knife4j，访问 /doc.html）
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("AI 智能书屋 API")
                .description("AI 智能书屋后端接口文档：认证、图书、借阅、AI 智能体、统计")
                .version("v1.0.0"));
    }
}
