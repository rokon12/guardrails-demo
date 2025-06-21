package ca.bazlur.guardrailsdemo;

import ca.bazlur.guardrailsdemo.guardrail.CustomerSupportJsonGuardrail;
import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
public class CustomerSupportController {

  private final CustomerSupportAssistant assistant;

  public CustomerSupportController(CustomerSupportAssistant assistant) {
    this.assistant = assistant;
  }

  @PostMapping("/chat")
  public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
    try {
      String response = assistant.chat(request.message());
      return ResponseEntity.ok(new ChatResponse(true, response, null));
    } catch (InputGuardrailException e) {
      return ResponseEntity.badRequest()
          .body(new ChatResponse(false, null, "Invalid input: " + e.getMessage()));
    } catch (OutputGuardrailException e) {
      return ResponseEntity.internalServerError()
          .body(new ChatResponse(false, null, "Unable to generate appropriate response"));
    }
  }

  @PostMapping("/analyze")
  public ResponseEntity<CustomerSupportJsonGuardrail.CustomerSupportResponse> analyze(@RequestBody ChatRequest request) {
    try {
      CustomerSupportJsonGuardrail.CustomerSupportResponse analysis = assistant.analyzeQuery(request.message());
      return ResponseEntity.ok(analysis);
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }
}

record ChatRequest(String message) {
}

record ChatResponse(boolean success, String response, String error) {
}