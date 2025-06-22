package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.GuardrailResult;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static dev.langchain4j.test.guardrail.GuardrailAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class SimpleGuardrailTest {

    private ContentSafetyInputGuardrail contentSafetyGuardrail;
    private PromptInjectionGuardrail promptInjectionGuardrail;
    private InputSanitizerGuardrail inputSanitizerGuardrail;
    private ProfessionalToneOutputGuardrail professionalToneGuardrail;
    private RateLimitingGuardrail rateLimitingGuardrail;
    private ContextAwareInputGuardrail contextAwareGuardrail;

    @BeforeEach
    void setUp() {
        contentSafetyGuardrail = new ContentSafetyInputGuardrail(1000); // Using config value
        promptInjectionGuardrail = new PromptInjectionGuardrail();
        inputSanitizerGuardrail = new InputSanitizerGuardrail();
        professionalToneGuardrail = new ProfessionalToneOutputGuardrail();
        rateLimitingGuardrail = new RateLimitingGuardrail(10, true); // Using config values
        contextAwareGuardrail = new ContextAwareInputGuardrail();
    }

    @Test
    @DisplayName("Content safety guardrail should allow clean messages")
    void contentSafetyGuardrailShouldAllowCleanMessages() {
        // Given
        UserMessage cleanMessage = UserMessage.from("How can I update my account settings?");

        // When
        InputGuardrailResult result = contentSafetyGuardrail.validate(cleanMessage);

        // Then
        assertThat(result)
                .isSuccessful()
                .hasResult(InputGuardrailResult.Result.SUCCESS);
    }

    @Test
    @DisplayName("Content safety guardrail should block prohibited words")
    void contentSafetyGuardrailShouldBlockProhibitedWords() {
        // Given
        UserMessage maliciousMessage = UserMessage.from("How can I hack into the system?");

        // When
        InputGuardrailResult result = contentSafetyGuardrail.validate(maliciousMessage);

        // Then
        assertThat(result)
                .hasFailures()
                .hasResult(InputGuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message contains prohibited content related to security threats.");
    }

    @Test
    @DisplayName("Content safety guardrail should reject overly long messages")
    void contentSafetyGuardrailShouldRejectOverlyLongMessages() {
        // Given
        String longMessage = "a".repeat(1001); // Exceeds 1000 char limit
        UserMessage message = UserMessage.from(longMessage);

        // When
        InputGuardrailResult result = contentSafetyGuardrail.validate(message);

        // Then
        assertThat(result)
                .hasFailures()
                .hasResult(InputGuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message is too long. Please keep it under 1000 characters.");
    }

    @Test
    @DisplayName("Prompt injection guardrail should allow normal questions")
    void promptInjectionGuardrailShouldAllowNormalQuestions() {
        // Given
        UserMessage normalMessage = UserMessage.from("What are your business hours?");

        // When
        InputGuardrailResult result = promptInjectionGuardrail.validate(normalMessage);

        // Then
        assertThat(result)
                .isSuccessful()
                .hasResult(InputGuardrailResult.Result.SUCCESS);
    }

    @Test
    @DisplayName("Input sanitizer should process messages without errors")
    void inputSanitizerShouldProcessMessagesWithoutErrors() {
        // Given
        UserMessage message = UserMessage.from("Hello! What's the price of your product?");

        // When
        InputGuardrailResult result = inputSanitizerGuardrail.validate(message);

        // Then
        assertThat(result)
                .isSuccessful()
                .hasResult(InputGuardrailResult.Result.SUCCESS_WITH_RESULT);
    }

    @Test
    @DisplayName("Professional tone guardrail should allow professional responses")
    void professionalToneGuardrailShouldAllowProfessionalResponses() {
        // Given
        AiMessage professionalResponse = AiMessage.from(
                "Thank you for your question. I'd be happy to help you with that."
        );

        // When
        OutputGuardrailResult result = professionalToneGuardrail.validate(professionalResponse);

        // Then
        assertThat(result)
                .isSuccessful()
                .hasResult(OutputGuardrailResult.Result.SUCCESS);
    }

    @Test
    @DisplayName("Professional tone guardrail should reject unprofessional responses")
    void professionalToneGuardrailShouldRejectUnprofessionalResponses() {
        // Given
        AiMessage unprofessionalResponse = AiMessage.from("idk, whatever dude");

        // When
        OutputGuardrailResult result = professionalToneGuardrail.validate(unprofessionalResponse);

        // Then
        assertThat(result)
                .hasFailures()
                .hasResult(OutputGuardrailResult.Result.FATAL)
                .hasSingleFailureWithMessage("Unprofessional tone detected");
    }

    @Test
    @DisplayName("Rate limiting guardrail should process initial requests successfully")
    void rateLimitingGuardrailShouldProcessInitialRequestsSuccessfully() {
        // Given
        UserMessage message = UserMessage.from("Can you help me?");

        // When
        InputGuardrailResult result = rateLimitingGuardrail.validate(message);

        // Then
        assertThat(result)
                .isSuccessful()
                .hasResult(InputGuardrailResult.Result.SUCCESS);
    }

    @Test
    @DisplayName("Context aware guardrail should process initial requests successfully")
    void contextAwareGuardrailShouldProcessInitialRequestsSuccessfully() {
        // Given
        UserMessage message = UserMessage.from("What is your return policy?");

        // When
        InputGuardrailResult result = contextAwareGuardrail.validate(message);

        // Then
        assertThat(result)
                .isSuccessful()
                .hasResult(InputGuardrailResult.Result.SUCCESS);
    }

    @Test
    @DisplayName("All input guardrails should handle short messages gracefully")
    void allInputGuardrailsShouldHandleShortMessagesGracefully() {
        // Given - Use a short but valid message instead of empty
        UserMessage shortMessage = UserMessage.from("Hi");

        // When & Then
        var result1 = contentSafetyGuardrail.validate(shortMessage);
        assertThat(result1)
                .hasFailures()
                .hasResult(InputGuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message is too short. Please provide more details.");

        var result2 = promptInjectionGuardrail.validate(shortMessage);
        assertThat(result2)
            .isSuccessful()
            .hasResult(InputGuardrailResult.Result.SUCCESS);

        var result3 = inputSanitizerGuardrail.validate(shortMessage);
        assertThat(result3)
            .isSuccessful()
            .hasResult(InputGuardrailResult.Result.SUCCESS_WITH_RESULT);

        var result4 = rateLimitingGuardrail.validate(shortMessage);
        assertThat(result4)
            .isSuccessful()
            .hasResult(InputGuardrailResult.Result.SUCCESS);

        var result5 = contextAwareGuardrail.validate(shortMessage);
        assertThat(result5)
            .isSuccessful()
            .hasResult(InputGuardrailResult.Result.SUCCESS);
    }

    @Test
    @DisplayName("Professional tone guardrail should handle empty responses gracefully")
    void professionalToneGuardrailShouldHandleEmptyResponsesGracefully() {
        // Given
        AiMessage emptyMessage = AiMessage.from("");

        // When
        OutputGuardrailResult result = professionalToneGuardrail.validate(emptyMessage);

        // Then
        assertThat(result)
                .hasFailures()
                .hasResult(OutputGuardrailResult.Result.FATAL)
                .hasSingleFailureWithMessage("Response lacks professional courtesy");
    }

    @Test
    @DisplayName("Guardrails should detect mixed language characters")
    void guardrailsShouldDetectMixedLanguageCharacters() {
        // Given
        UserMessage unicodeMessage = UserMessage.from("I would like to visit your café tomorrow afternoon to discuss the new menu items. Thank you!");

        // When & Then
        var result1 = contentSafetyGuardrail.validate(unicodeMessage);
        assertThat(result1)
                .isSuccessful()
                .hasResult(InputGuardrailResult.Result.SUCCESS);

        var result2 = promptInjectionGuardrail.validate(unicodeMessage);
        assertThat(result2)
                .hasFailures()
                .hasResult(InputGuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Please submit your question in a single language.");

        var result3 = inputSanitizerGuardrail.validate(unicodeMessage);
        assertThat(result3)
                .isSuccessful()
                .hasResult(InputGuardrailResult.Result.SUCCESS_WITH_RESULT);
    }

    @Test
    @DisplayName("Professional tone guardrail should handle unicode in responses")
    void professionalToneGuardrailShouldHandleUnicodeInResponses() {
        // Given
        AiMessage unicodeResponse = AiMessage.from("Thank you! 😊 I'd be happy to help with café information.");

        // When
        OutputGuardrailResult result = professionalToneGuardrail.validate(unicodeResponse);

        // Then
        assertThat(result)
            .isSuccessful()
            .hasResult(OutputGuardrailResult.Result.SUCCESS);
    }
}
