package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PromptInjectionGuardrail implements InputGuardrail {
  private static final Pattern[] INJECTION_PATTERNS = {
      Pattern.compile("ignore\\s+previous\\s+instructions", Pattern.CASE_INSENSITIVE),
      Pattern.compile("disregard\\s+all\\s+rules", Pattern.CASE_INSENSITIVE),
      Pattern.compile("you\\s+are\\s+now", Pattern.CASE_INSENSITIVE),
      Pattern.compile("new\\s+instructions:", Pattern.CASE_INSENSITIVE)
  };

  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    String text = userMessage.singleText();

    for (Pattern pattern : INJECTION_PATTERNS) {
      if (pattern.matcher(text).find()) {
        return fatal("Potential prompt injection detected. Please ask a genuine question.");
      }
    }

    return success();
  }
}
