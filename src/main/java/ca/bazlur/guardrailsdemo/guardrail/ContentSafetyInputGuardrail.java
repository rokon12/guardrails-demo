package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContentSafetyInputGuardrail implements InputGuardrail {
  private static final List<String> PROHIBITED_WORDS = List.of(
      "hack", "exploit", "bypass", "illegal", "fraud"
  );

  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    String text = userMessage.singleText().toLowerCase();
    if (text.length() > 1000) {
      return failure("Your message is too long. Please keep it under 1000 characters.");
    }
    if (text.isBlank()) {
      return failure("Your message cannot be empty.");
    }
    if (text.length() < 5) {
      return failure("Your message is too short. Please provide more details.");
    }

    // Check for prohibited words
    for (String word : PROHIBITED_WORDS) {
      if (text.contains(word)) {
        return failure("Your message contains prohibited content: " + word);
      }
    }
    return success();
  }
}
