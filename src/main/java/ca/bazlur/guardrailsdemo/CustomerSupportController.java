package ca.bazlur.guardrailsdemo;

import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
      log.info("Invalid input {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(new ChatResponse(false, null, "Invalid input: " + e.getMessage()));
    } catch (OutputGuardrailException e) {
      log.info("Invalid output {}", e.getMessage());
      return ResponseEntity.internalServerError()
          .body(new ChatResponse(false, null, "Unable to generate appropriate response"));
    }
  }

}

record ChatRequest(String message) {
}

record ChatResponse(boolean success, String response, String error) {
}