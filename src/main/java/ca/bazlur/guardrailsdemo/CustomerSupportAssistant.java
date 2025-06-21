package ca.bazlur.guardrailsdemo;


import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface CustomerSupportAssistant {
  @SystemMessage("You are a helpful customer support assistant. Respond professionally and helpfully to customer queries.")
  String chat(@UserMessage String message);
}
