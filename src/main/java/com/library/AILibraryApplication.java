package com.library;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 智能书屋后端启动类
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("com.library.mapper")
public class AILibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(AILibraryApplication.class, args);
    }

}
