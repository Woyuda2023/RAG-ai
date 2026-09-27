package com.example.aicodeassistant.processing;

import com.example.aicodeassistant.config.AssistantProperties;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 结果处理：控制进入 LLM 上下文的工具结果长度，避免 token 超限。
 *
 * <ul>
 *   <li>确定性截断：保留头部 60% 与尾部 40%，中间以省略标记连接；</li>
 *   <li>可选摘要：配置 summarize-when-truncated=true 时，截断后再调用 LLM 压缩要点。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResultTruncator {

    private final AssistantProperties properties;
    private final ChatLanguageModel chatLanguageModel;

    /** 确定性截断。 */
    public TruncationResult truncate(String content, int maxChars) {
        if (content == null) {
            return new TruncationResult("", 0, false);
        }
        int length = content.length();
        if (length <= maxChars) {
            return new TruncationResult(content, length, false);
        }
        int head = (int) (maxChars * 0.6);
        int tail = maxChars - head;
        String marker = String.format("\n...[已截断，原始长度 %d 字符，省略 %d 字符]...\n", length, length - maxChars);
        String result = content.substring(0, head) + marker + content.substring(length - tail);
        return new TruncationResult(result, length, true);
    }

    /** 先截断，必要时用 LLM 生成摘要（可选项）。 */
    public String truncateWithSummary(String content) {
        return truncateWithSummary(content, properties.getAgent().getMaxResultChars());
    }

    public String truncateWithSummary(String content, int maxChars) {
        TruncationResult result = truncate(content, maxChars);
        if (!result.truncated()) {
            return content;
        }
        if (properties.getAgent().isSummarizeWhenTruncated()) {
            try {
                var response = chatLanguageModel.generate(
                        SystemMessage.from("你是信息压缩助手，只输出要点，不输出无关内容。"),
                        UserMessage.from("请用不超过 200 字概括以下长内容的要点：\n" + result.content()));
                String summary = response.content().text();
                if (summary != null && !summary.isBlank()) {
                    return summary + "\n[原内容过长，以上为 LLM 摘要，省略 " + (result.originalLength() - maxChars) + " 字符]";
                }
            } catch (Exception e) {
                log.warn("LLM 摘要失败，回退到截断结果: {}", e.getMessage());
            }
        }
        return result.content();
    }

    /** 截断结果 */
    public record TruncationResult(String content, int originalLength, boolean truncated) {
    }
}
