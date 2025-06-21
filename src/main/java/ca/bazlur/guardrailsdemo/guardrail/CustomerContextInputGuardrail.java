package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.stereotype.Component;

@Component
public class CustomerContextInputGuardrail implements InputGuardrail {
  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    String text = userMessage.singleText();

    if (!containCustomerInfo(text)) {
      return failure("Please provide your name and ticket number to begin");
    }
    return success();

  }

  private boolean containCustomerInfo(String text) {
    return text.toLowerCase().contains("name") ||
        text.matches(".*\\b[A-Z]{2,3}-\\d{4,6}\\b.*");
  }
}
