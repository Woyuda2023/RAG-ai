package com.example.aicodeassistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * AI 代码助手启动类。
 *
 * <p>技术栈：Spring Boot 3 / LangChain4j / 通义千问 API（OpenAI 兼容）/ MySQL + MyBatis。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.example.aicodeassistant.mapper")
public class AiCodeAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeAssistantApplication.class, args);
    }
}
