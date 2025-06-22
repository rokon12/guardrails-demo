package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static dev.langchain4j.test.guardrail.GuardrailAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class CustomerSupportJsonGuardrailTest {

    private CustomerSupportJsonGuardrail guardrail;

    @BeforeEach
    void setUp() {
        guardrail = new CustomerSupportJsonGuardrail();
    }

    @Test
    void shouldAllowValidJsonResponse() {
        // Given
        String validJson = """
            {
                "answer": "Customer wants to reset their password",
                "category": "ACCOUNT",
                "confidence": 0.95,
                "intent": "password_reset",
                "priority": "MEDIUM",
                "sentiment": "NEUTRAL",
                "suggestedResponse": "Guide them through the password reset process"
            }
            """;

        // When
        OutputGuardrailResult result = guardrail.validate(AiMessage.from(validJson));

        // Then
        assertThat(result)
                .isSuccessful()
                .hasResult(OutputGuardrailResult.Result.SUCCESS_WITH_RESULT);
    }

    @Test
    void shouldHandleJsonInCodeBlock() {
        // Given
        String jsonInCodeBlock = """
            ```json
            {
                "answer": "Customer needs billing help",
                "category": "BILLING",
                "confidence": 0.85,
                "intent": "billing_inquiry",
                "priority": "HIGH",
                "sentiment": "NEGATIVE"
            }
            ```
            """;

        // When
        OutputGuardrailResult result = guardrail.validate(AiMessage.from(jsonInCodeBlock));

        // Then
        assertThat(result)
                .isSuccessful()
                .hasResult(OutputGuardrailResult.Result.SUCCESS_WITH_RESULT);
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        // Given
        String[] invalidJsons = {
            // Missing answer
            """
            {
                "category": "ACCOUNT",
                "confidence": 0.95
            }
            """,
            // Missing category
            """
            {
                "answer": "Customer needs help",
                "confidence": 0.95
            }
            """,
            // Missing confidence
            """
            {
                "answer": "Customer needs help",
                "category": "ACCOUNT"
            }
            """
        };

        // When & Then
        for (String json : invalidJsons) {
            OutputGuardrailResult result = guardrail.validate(AiMessage.from(json));
            assertThat(result)
                .as("Should reject JSON missing required fields: " + json)
                .hasFailures()
                .hasResult(OutputGuardrailResult.Result.FAILURE);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "INVALID_CATEGORY",
        "unknown",
        "support",
        "help"
    })
    void shouldRejectInvalidCategories(String invalidCategory) {
        // Given
        String json = String.format("""
            {
                "answer": "Customer needs help",
                "category": "%s",
                "confidence": 0.95
            }
            """, invalidCategory);

        // When
        OutputGuardrailResult result = guardrail.validate(AiMessage.from(json));

        // Then
        assertThat(result)
                .hasFailures()
                .hasResult(OutputGuardrailResult.Result.FAILURE);
        org.assertj.core.api.Assertions.assertThat(result.toString())
                .contains("Category must be one of:");
    }

    @Test
    void shouldRejectInvalidConfidenceValues() {
        // Given
        String[] invalidJsons = {
            // Negative confidence
            """
            {
                "answer": "Customer needs help",
                "category": "ACCOUNT",
                "confidence": -0.5
            }
            """,
            // Confidence > 1.0
            """
            {
                "answer": "Customer needs help",
                "category": "ACCOUNT",
                "confidence": 1.5
            }
            """
        };

        // When & Then
        for (String json : invalidJsons) {
            OutputGuardrailResult result = guardrail.validate(AiMessage.from(json));
            assertThat(result)
                .as("Should reject invalid confidence value: " + json)
                .hasFailures()
                .hasResult(OutputGuardrailResult.Result.FAILURE);
            org.assertj.core.api.Assertions.assertThat(result.toString())
                .contains("Confidence must be between 0.0 and 1.0");
        }
    }

    @Test
    void shouldHandleEmptyOrNullInput() {
        // Given
        String[] invalidInputs = {
            "",
            " ",
            null,
            "not a json",
            "{invalid json}",
            "[]"  // valid JSON but not an object
        };

        // When & Then
        for (String input : invalidInputs) {
            if (input != null) {
                OutputGuardrailResult result = guardrail.validate(AiMessage.from(input));
                assertThat(result)
                    .as("Should reject invalid input: " + input)
                    .hasFailures()
                    .hasResult(OutputGuardrailResult.Result.FATAL);
            } else {
                // Test null input directly
                OutputGuardrailResult result = guardrail.validate(AiMessage.from(""));
                assertThat(result)
                    .as("Should reject null/empty input")
                    .hasFailures()
                    .hasResult(OutputGuardrailResult.Result.FATAL);
            }
        }
    }
}
