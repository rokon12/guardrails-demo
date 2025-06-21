package ca.bazlur.guardrailsdemo.guardrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.JsonExtractorOutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerSupportJsonGuardrail extends JsonExtractorOutputGuardrail<CustomerSupportJsonGuardrail.CustomerSupportResponse> {

  private static final List<String> VALID_CATEGORIES = List.of("ACCOUNT", "BILLING", "TECHNICAL", "PRODUCT", "GENERAL");

  private final ObjectMapper objectMapper = new ObjectMapper();

  public CustomerSupportJsonGuardrail() {
    super(CustomerSupportResponse.class);
  }

  @Override
  public OutputGuardrailResult validate(AiMessage message) {
    OutputGuardrailResult result = super.validate(message);

    if (result.isSuccess()) {
      try {
        // Attempt to extract the response from the AI message
        CustomerSupportResponse response = extractFrom(message);
        if (response != null) {
          // Validate the extracted response
          if (response.getAnswer() == null || response.getAnswer().trim().isEmpty()) {
            return failure("Analysis must include a non-empty answer");
          }

          if (response.getCategory() == null || !VALID_CATEGORIES.contains(response.getCategory().toUpperCase())) {
            return failure("Category must be one of: " + String.join(", ", VALID_CATEGORIES));
          }

          if (response.getConfidence() == null || response.getConfidence() < 0.0 || response.getConfidence() > 1.0) {
            return failure("Confidence must be between 0.0 and 1.0");
          }

          // Normalize category to uppercase
          response.setCategory(response.getCategory().toUpperCase());
        }
      } catch (Exception e) {
        return failure("Failed to validate analysis response: " + e.getMessage());
      }
    }

    return result;
  }

  private CustomerSupportResponse extractFrom(AiMessage message) {
    if (message == null) {
      return null;
    }

    String raw = message.text();            // full text returned by the LLM
    if (raw == null || raw.isBlank()) {
      return null;
    }

    raw = raw.trim();
    if (raw.startsWith("```")) {
      int firstBrace = raw.indexOf('{');
      int lastBrace = raw.lastIndexOf('}');
      if (firstBrace >= 0 && lastBrace >= firstBrace) {
        raw = raw.substring(firstBrace, lastBrace + 1);
      }
    }

    // Fallback: look for the first JSON object in the text
    int start = raw.indexOf('{');
    int end = raw.lastIndexOf('}');
    if (start < 0 || end <= start) {
      return null; // nothing that looks like JSON
    }

    String json = raw.substring(start, end + 1);

    try {
      return objectMapper.readValue(json, CustomerSupportResponse.class);
    } catch (Exception e) {
      // If parsing fails, treat it as no valid response instead of throwing
      return null;
    }
  }

  @Setter
  @Getter
  public static class CustomerSupportResponse {
    private String answer;
    private String category;
    private Double confidence;
    private String intent;
    private String priority;
    private String sentiment;
    private String suggestedResponse;
  }
}