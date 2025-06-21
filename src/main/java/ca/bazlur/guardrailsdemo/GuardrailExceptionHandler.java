package ca.bazlur.guardrailsdemo;

import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GuardrailExceptionHandler {

    @ExceptionHandler(InputGuardrailException.class)
    public ResponseEntity<ErrorResponse> handleInputGuardrailException(InputGuardrailException e, WebRequest request) {
        log.warn("Input guardrail validation failed: {}", e.getMessage());

        // Log exception details for monitoring
        log.warn("Input guardrail exception of type: {}", e.getClass().getSimpleName());

        // Get the error message from the exception
        String errorMessage = e.getMessage() != null ? e.getMessage() : "Input validation failed";
        List<String> failures = List.of(errorMessage);
        
        // Extract guardrail information from exception message if available
        String failedGuardrail = extractGuardrailName(e.getMessage());

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "INPUT_VALIDATION_FAILED",
                        "Your input did not pass validation",
                        failures,
                        Instant.now(),
                        Map.of(
                            "guardrail", failedGuardrail,
                            "path", extractPath(request)
                        )
                ));
    }

    @ExceptionHandler(OutputGuardrailException.class)
    public ResponseEntity<ErrorResponse> handleOutputGuardrailException(OutputGuardrailException e, WebRequest request) {
        log.error("Output guardrail validation failed after retries: {}", e.getMessage());

        // Log exception details for monitoring
        log.error("Output guardrail exception of type: {}", e.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        "OUTPUT_GENERATION_FAILED",
                        "Unable to generate an appropriate response. Please try again later.",
                        List.of("The AI response did not meet quality standards"),
                        Instant.now(),
                        Map.of(
                            "retries", "exhausted",
                            "path", extractPath(request)
                        )
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e, WebRequest request) {
        log.error("Unexpected error in AI service", e);

        // Log exception details for monitoring
        log.error("Generic exception of type: {}", e.getClass().getSimpleName());

        // Don't expose internal error details in production
        List<String> details = List.of();
        if (log.isDebugEnabled()) {
            details = List.of(e.getMessage() != null ? e.getMessage() : "No message available");
        }

        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred. Please try again later.",
                        details,
                        Instant.now(),
                        Map.of(
                            "requestId", "unknown",
                            "path", extractPath(request)
                        )
                ));
    }

    private String extractGuardrailName(String message) {
        if (message == null) return "Unknown";
        // Try to extract guardrail name from common patterns
        if (message.contains("ContentSafety")) return "ContentSafetyInputGuardrail";
        if (message.contains("PromptInjection")) return "PromptInjectionGuardrail";
        if (message.contains("ProfessionalTone")) return "ProfessionalToneOutputGuardrail";
        if (message.contains("Hallucination")) return "HallucinationDetectionGuardrail";
        return "Unknown";
    }
    
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replaceFirst("uri=", "");
    }

    public record ErrorResponse(
            String code,
            String message,
            List<String> details,
            Instant timestamp,
            Map<String, String> metadata
    ) {
        // Backward compatibility constructor
        public ErrorResponse(String code, String message, List<String> details) {
            this(code, message, details, Instant.now(), Map.of());
        }
    }
}