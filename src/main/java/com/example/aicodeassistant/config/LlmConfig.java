package com.example.aicodeassistant.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LLM 接入配置：通过 OpenAI 兼容协议接入云端大模型 API。
 *
 * <p>DeepSeek、通义千问（DashScope）、Kimi（Moonshot）、智谱 GLM、豆包（火山方舟）等
 * 主流厂商均提供 OpenAI 兼容端点，切换厂商只需修改 application.yml 中的 llm.* 配置。</p>
 */
@Configuration
public class LlmConfig {

    @Value("${llm.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model-name:deepseek-chat}")
    private String modelName;

    @Value("${llm.temperature:0.2}")
    private Double temperature;

    @Value("${llm.timeout:120s}")
    private Duration timeout;

    @Value("${llm.max-retries:2}")
    private Integer maxRetries;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(timeout)
                .maxRetries(maxRetries)
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
