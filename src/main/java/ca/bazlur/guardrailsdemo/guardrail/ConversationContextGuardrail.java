package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.memory.ChatMemory;
import org.springframework.stereotype.Component;

@Component
public class ConversationContextGuardrail implements InputGuardrail {
  private static final int MAX_QUESTIONS_PER_TOPIC = 5;

  @Override
  public InputGuardrailResult validate(InputGuardrailRequest params) {
    ChatMemory memory = params.requestParams().chatMemory();

    if (memory == null || memory.messages().isEmpty()) {
      return success();
    }

    String currentQuestion = params.userMessage().singleText();

    for (ChatMessage message : memory.messages()) {
      getMessage(message);
    }

    long similarQuestions = memory.messages().stream()
        .filter(msg -> getMessage(msg).toLowerCase().contains(currentQuestion.toLowerCase()))
        .count();
    if (similarQuestions > MAX_QUESTIONS_PER_TOPIC) {
      return failure("You've asked similar questions multiple times. Please try a different topic.");
    }

    return success();
  }

  private String getMessage(ChatMessage message) {
    return switch (message) {
      case UserMessage userMessage -> userMessage.singleText();
      case AiMessage aiMessage -> aiMessage.text();
      default -> message.toString();
    };
  }
}
