package ca.bazlur.guardrailsdemo;

import ca.bazlur.guardrailsdemo.guardrail.*;
import dev.langchain4j.guardrail.config.OutputGuardrailsConfig;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiServiceConfiguration {

    @Bean
    public ChatModel chatModel(@Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
                               @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName,
                               @Value("${langchain4j.open-ai.chat-model.timeout}") Duration timeout,
                               @Value("${langchain4j.open-ai.chat-model.temperature}") double temperature,
                               @Value("${langchain4j.open-ai.chat-model.log-requests}") boolean logRequests,
                               @Value("${langchain4j.open-ai.chat-model.log-responses}") boolean logResponses,
                               @Value("${langchain4j.open-ai.chat-model.max-tokens}") int maxTokens) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .temperature(temperature)
                .maxCompletionTokens(maxTokens)
                .timeout(timeout)
                .build();
    }

    @Bean
    public CustomerSupportAssistant customerSupportAssistant(
            ChatModel chatModel,
            ContentSafetyInputGuardrail contentSafetyInputGuardrail,
            ConversationContextGuardrail conversationContextGuardrail,
            PromptInjectionGuardrail injectionGuard,
            ProfessionalToneOutputGuardrail toneGuard,
            HallucinationDetectionGuardrail hallucinationDetectionGuardrail,
            InputSanitizerGuardrail inputSanitizerGuardrail,
            ContextAwareInputGuardrail contextAwareInputGuardrail,
            RateLimitingGuardrail rateLimitingGuardrail,
            @Value("${app.guardrails.output.max-retries}") int maxRetries) {

        OutputGuardrailsConfig outputConfig = OutputGuardrailsConfig.builder()
                .maxRetries(maxRetries)
                .build();

        return AiServices.builder(CustomerSupportAssistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .inputGuardrails(
                        contentSafetyInputGuardrail,
                        inputSanitizerGuardrail,
                        conversationContextGuardrail,
                        injectionGuard,
                        contextAwareInputGuardrail,
                        rateLimitingGuardrail
                )
                .outputGuardrails(toneGuard, hallucinationDetectionGuardrail)
                .outputGuardrailsConfig(outputConfig)
                .build();
    }
}