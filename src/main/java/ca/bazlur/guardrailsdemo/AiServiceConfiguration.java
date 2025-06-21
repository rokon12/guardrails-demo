package ca.bazlur.guardrailsdemo;

import ca.bazlur.guardrailsdemo.guardrail.*;
import dev.langchain4j.guardrail.config.OutputGuardrailsConfig;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiServiceConfiguration {

  @Bean
  public ChatModel chatModel() {
    return OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o")
        .logRequests(true)
        .logResponses(true)
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
      CustomerSupportJsonGuardrail customerSupportJsonGuardrail) {

    OutputGuardrailsConfig outputConfig = OutputGuardrailsConfig.builder()
        .maxRetries(5)
        .build();

    return AiServices.builder(CustomerSupportAssistant.class)
        .chatModel(chatModel)
        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
        .inputGuardrails(contentSafetyInputGuardrail, inputSanitizerGuardrail, conversationContextGuardrail, injectionGuard)
        .outputGuardrails(toneGuard, hallucinationDetectionGuardrail, customerSupportJsonGuardrail)
        .outputGuardrailsConfig(outputConfig)
        .build();
  }
}