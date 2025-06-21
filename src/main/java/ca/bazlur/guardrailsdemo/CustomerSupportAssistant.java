package ca.bazlur.guardrailsdemo;


import ca.bazlur.guardrailsdemo.guardrail.CustomerSupportJsonGuardrail;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface CustomerSupportAssistant {
  @SystemMessage("You are a helpful customer support assistant. Respond professionally and helpfully to customer queries.")
  String chat(@UserMessage String message);

  @SystemMessage("""
      Analyze the customer query and respond with a JSON object containing:
      - answer: A brief summary of what the customer needs (required)
      - category: One of [ACCOUNT, BILLING, TECHNICAL, PRODUCT, GENERAL] (required)
      - confidence: A number between 0.0 and 1.0 indicating confidence in the categorization (required)
      - intent: The main intent behind the query (optional)
      - priority: One of [LOW, MEDIUM, HIGH] based on urgency (optional) 
      - sentiment: One of [POSITIVE, NEUTRAL, NEGATIVE] (optional)
      - suggestedResponse: A brief suggested response direction (optional)
      
      Example:
      {"answer": "Customer wants to reset password", "category": "ACCOUNT", "confidence": 0.95, "intent": "password_reset", "priority": "MEDIUM", "sentiment": "NEUTRAL"}
      """)
  CustomerSupportJsonGuardrail.CustomerSupportResponse analyzeQuery(@UserMessage String query);
}
