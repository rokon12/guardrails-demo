package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
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
        contentSafetyGuardrail = new ContentSafetyInputGuardrail();
        promptInjectionGuardrail = new PromptInjectionGuardrail();
        inputSanitizerGuardrail = new InputSanitizerGuardrail();
        professionalToneGuardrail = new ProfessionalToneOutputGuardrail();
        rateLimitingGuardrail = new RateLimitingGuardrail();
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
        if (!result.isSuccess()) {
            System.out.println("Clean message failed: " + result.failures().getFirst().message());
        }
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Content safety guardrail should block prohibited words")
    void contentSafetyGuardrailShouldBlockProhibitedWords() {
        // Given
        UserMessage maliciousMessage = UserMessage.from("How can I hack into the system?");

        // When
        InputGuardrailResult result = contentSafetyGuardrail.validate(maliciousMessage);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.failures().getFirst().message()).contains("prohibited content");
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
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.failures().getFirst().message()).contains("too long");
    }

    @Test
    @DisplayName("Prompt injection guardrail should allow normal questions")
    void promptInjectionGuardrailShouldAllowNormalQuestions() {
        // Given
        UserMessage normalMessage = UserMessage.from("What are your business hours?");

        // When
        InputGuardrailResult result = promptInjectionGuardrail.validate(normalMessage);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Input sanitizer should process messages without errors")
    void inputSanitizerShouldProcessMessagesWithoutErrors() {
        // Given
        UserMessage message = UserMessage.from("Hello! What's the price of your product?");

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            InputGuardrailResult result = inputSanitizerGuardrail.validate(message);
            assertThat(result).isNotNull();
        });
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
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Professional tone guardrail should reject unprofessional responses")
    void professionalToneGuardrailShouldRejectUnprofessionalResponses() {
        // Given
        AiMessage unprofessionalResponse = AiMessage.from("idk, whatever dude");

        // When
        OutputGuardrailResult result = professionalToneGuardrail.validate(unprofessionalResponse);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.failures().getFirst().message()).contains("professional");
    }

    @Test
    @DisplayName("Rate limiting guardrail should process initial requests successfully")
    void rateLimitingGuardrailShouldProcessInitialRequestsSuccessfully() {
        // Given
        UserMessage message = UserMessage.from("Can you help me?");

        // When
        InputGuardrailResult result = rateLimitingGuardrail.validate(message);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Context aware guardrail should process initial requests successfully")
    void contextAwareGuardrailShouldProcessInitialRequestsSuccessfully() {
        // Given
        UserMessage message = UserMessage.from("What is your return policy?");

        // When
        InputGuardrailResult result = contextAwareGuardrail.validate(message);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("All input guardrails should handle short messages gracefully")
    void allInputGuardrailsShouldHandleShortMessagesGracefully() {
        // Given - Use a short but valid message instead of empty
        UserMessage shortMessage = UserMessage.from("Hi");

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            contentSafetyGuardrail.validate(shortMessage);
            promptInjectionGuardrail.validate(shortMessage);
            inputSanitizerGuardrail.validate(shortMessage);
            rateLimitingGuardrail.validate(shortMessage);
            contextAwareGuardrail.validate(shortMessage);
        });
    }

    @Test
    @DisplayName("Professional tone guardrail should handle empty responses gracefully")
    void professionalToneGuardrailShouldHandleEmptyResponsesGracefully() {
        // Given
        AiMessage emptyMessage = AiMessage.from("");

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            OutputGuardrailResult result = professionalToneGuardrail.validate(emptyMessage);
            assertThat(result).isNotNull();
        });
    }

    @Test
    @DisplayName("Guardrails should handle unicode characters")
    void guardrailsShouldHandleUnicodeCharacters() {
        // Given
        UserMessage unicodeMessage = UserMessage.from("Hello! 👋 Can you help with café reservations?");

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            InputGuardrailResult result1 = contentSafetyGuardrail.validate(unicodeMessage);
            InputGuardrailResult result2 = promptInjectionGuardrail.validate(unicodeMessage);
            InputGuardrailResult result3 = inputSanitizerGuardrail.validate(unicodeMessage);

            assertThat(result1).isNotNull();
            assertThat(result2).isNotNull();
            assertThat(result3).isNotNull();
        });
    }

    @Test
    @DisplayName("Professional tone guardrail should handle unicode in responses")
    void professionalToneGuardrailShouldHandleUnicodeInResponses() {
        // Given
        AiMessage unicodeResponse = AiMessage.from("Thank you! 😊 I'd be happy to help with café information.");

        // When & Then
        assertThatNoException().isThrownBy(() -> {
            OutputGuardrailResult result = professionalToneGuardrail.validate(unicodeResponse);
            assertThat(result).isNotNull();
        });
    }
}