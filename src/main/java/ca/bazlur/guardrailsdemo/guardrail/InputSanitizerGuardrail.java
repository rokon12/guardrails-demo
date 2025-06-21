package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.stereotype.Component;

@Component
public class InputSanitizerGuardrail implements InputGuardrail {

  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    String text = userMessage.singleText();

    // Remove excessive whitespace
    text = text.replaceAll("\\s+", " ").trim();

    // Remove special characters that might cause issues
    text = text.replaceAll("[<>{}\\[\\]|\\\\]", "");

    // Limit length
    if (text.length() > 500) {
      text = text.substring(0, 500) + "...";
    }

    // Return success with the sanitized message
    return successWith(text);
  }
}
