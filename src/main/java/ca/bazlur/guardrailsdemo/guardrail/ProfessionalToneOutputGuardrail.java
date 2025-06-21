package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProfessionalToneOutputGuardrail implements OutputGuardrail {
  private static final List<String> UNPROFESSIONAL_PHRASES = List.of(
      "that's weird", "that's dumb", "whatever", "i don't know"
  );

  private static final List<String> REQUIRED_ELEMENTS = List.of(
      "thank you",
      "please",
      "happy to help"
  );

  @Override
  public OutputGuardrailResult validate(AiMessage responseFromLLM) {
    String text = responseFromLLM.text().toLowerCase();

    for (String unprofessionalPhrase : UNPROFESSIONAL_PHRASES) {
      if (text.contains(unprofessionalPhrase)) {
        return reprompt("Unprofessional tone detected",
            "Please maintain a professional and helpful tone");
      }
    }


    if (text.length() > 1000) {
      return reprompt("Response too long",
          "Please keep your response under 1000 characters.");
    }

    boolean hasCourtesy = REQUIRED_ELEMENTS.stream()
        .anyMatch(text::contains);

    if (!hasCourtesy) {
      return reprompt(
          "Response lacks professional courtesy",
          "Please include polite and helpful language in your response."
      );
    }

    return success();
  }
}
