package ca.bazlur.guardrailsdemo;


import ca.bazlur.guardrailsdemo.guardrail.CustomerSupportJsonGuardrail;
import dev.langchain4j.service.UserMessage;

public interface CustomerSupportAssistant {
  String chat(@UserMessage String message);

  CustomerSupportJsonGuardrail.CustomerSupportResponse analyzeQuery(@UserMessage String query);
}
