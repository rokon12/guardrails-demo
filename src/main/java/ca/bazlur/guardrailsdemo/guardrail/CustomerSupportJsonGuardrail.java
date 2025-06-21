package ca.bazlur.guardrailsdemo.guardrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.JsonExtractorOutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;


@Component
public class CustomerSupportJsonGuardrail extends JsonExtractorOutputGuardrail<CustomerSupportJsonGuardrail.CustomerSupportResponse> {

  public CustomerSupportJsonGuardrail() {
    super(CustomerSupportResponse.class);
  }

  @Setter
  @Getter
  static public class CustomerSupportResponse {
    // Getters and setters
    private String answer;
    private String category;
    private Double confidence;
  }

}