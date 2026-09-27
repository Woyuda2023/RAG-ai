package com.example.aicodeassistant.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LLM 接入配置：通过 Ollama 加载本地大模型（如 qwen2.5-coder 系列）。
 */
@Configuration
public class LlmConfig {

    @Value("${langchain4j.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name:qwen2.5-coder:14b}")
    private String modelName;

    @Value("${langchain4j.ollama.chat-model.temperature:0.2}")
    private Double temperature;

    @Value("${langchain4j.ollama.chat-model.timeout:120s}")
    private Duration timeout;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(timeout)
                .build();
    }
}
