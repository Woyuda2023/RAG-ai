package com.example.aicodeassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * AI 代码助手启动类。
 *
 * <p>技术栈：Spring Boot 3 / LangChain4j / Ollama / MySQL。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AiCodeAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeAssistantApplication.class, args);
    }
}
