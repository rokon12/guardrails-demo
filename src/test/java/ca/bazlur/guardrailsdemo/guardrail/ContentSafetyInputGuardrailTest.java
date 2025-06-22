package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.GuardrailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static dev.langchain4j.test.guardrail.GuardrailAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentSafetyInputGuardrailTest {
    private ContentSafetyInputGuardrail guardrail;

    @BeforeEach
    void setUp() {
        guardrail = new ContentSafetyInputGuardrail(100); // Max length of 100 characters
    }

    @Test
    void shouldAcceptValidInput() {
        var result = guardrail.validate(UserMessage.from("Hello, I need help with my account settings"));
        assertThat(result)
                .isSuccessful()
                .hasResult(GuardrailResult.Result.SUCCESS);
    }

    @Test
    void shouldRejectEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            guardrail.validate(UserMessage.from(""));
        });
    }

    @Test
    void shouldRejectBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            guardrail.validate(UserMessage.from("   "));
        });
    }

    @Test
    void shouldRejectTooShortInput() {
        var result = guardrail.validate(UserMessage.from("Hi"));
        assertThat(result)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message is too short. Please provide more details.");
    }

    @Test
    void shouldRejectTooLongInput() {
        String longInput = "a".repeat(101);
        var result = guardrail.validate(UserMessage.from(longInput));
        assertThat(result)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message is too long. Please keep it under 100 characters.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "hack the system",
        "how to exploit vulnerability",
        "bypass security",
        "crack the password",
        "install malware",
        "phishing techniques",
        "cr@ck the password"  // Added here because it contains prohibited word "password"
    })
    void shouldRejectProhibitedWords(String input) {
        var result = guardrail.validate(UserMessage.from(input));
        assertThat(result)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message contains prohibited content related to security threats.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "h4ck the system",
        "how to h@ck",
        "byp@ss security",
        "m@lw@re installation"
    })
    void shouldRejectObfuscatedPatterns(String input) {
        var result = guardrail.validate(UserMessage.from(input));
        assertThat(result)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message contains potentially harmful content patterns.");
    }

    @Test
    void shouldRejectSuspiciousCharacterSubstitutions() {
        var result = guardrail.validate(UserMessage.from("H3!!0 @#$%^ &*()_ +"));
        assertThat(result)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message contains suspicious character substitutions.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Can you help me with my login issue?",
        "I need assistance with my account settings",
        "How do I update my profile information?",
        "What are the steps to contact support?"
    })
    void shouldAcceptVariousValidInputs(String input) {
        var result = guardrail.validate(UserMessage.from(input));
        assertThat(result)
                .isSuccessful()
                .hasResult(GuardrailResult.Result.SUCCESS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "how to hack the system",
        "teach me to exploit",
        "show me how to bypass",
        "HOW TO HACK",
        "Teach Me To EXPLOIT",
        "Show ME how TO bypass"
    })
    void shouldRejectInstructionalPatterns(String input) {
        var result = guardrail.validate(UserMessage.from(input));
        assertThat(result)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message contains prohibited content related to security threats.");
    }

    @Test
    void shouldHandleCaseSensitivity() {
        var result1 = guardrail.validate(UserMessage.from("HACK the System"));
        var result2 = guardrail.validate(UserMessage.from("ExPlOiT vulnerability"));
        var result3 = guardrail.validate(UserMessage.from("ByPaSs security"));

        assertThat(result1)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message contains prohibited content related to security threats.");
        assertThat(result2)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message contains prohibited content related to security threats.");
        assertThat(result3)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message contains prohibited content related to security threats.");
    }

    @Test
    void shouldHandleSpecialCharacterRatioBoundary() {
        // Exactly 15% special characters (3 out of 20 chars)
        var result1 = guardrail.validate(UserMessage.from("Hello@World#Test$ing"));
        assertThat(result1)
                .isSuccessful()
                .hasResult(GuardrailResult.Result.SUCCESS);

        // Just over 15% special characters (4 out of 20 chars)
        var result2 = guardrail.validate(UserMessage.from("Hello@World#Test$ing%"));
        assertThat(result2)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message contains suspicious character substitutions.");
    }

    @Test
    void shouldHandleLengthBoundaries() {
        // Exactly 5 characters
        var result1 = guardrail.validate(UserMessage.from("Hello"));
        assertThat(result1)
                .isSuccessful()
                .hasResult(GuardrailResult.Result.SUCCESS);

        // 4 characters (too short)
        var result2 = guardrail.validate(UserMessage.from("Help"));
        assertThat(result2)
                .hasFailures()
                .hasResult(GuardrailResult.Result.FAILURE)
                .hasSingleFailureWithMessage("Your message is too short. Please provide more details.");

        // Exactly max length
        var result3 = guardrail.validate(UserMessage.from("a".repeat(100)));
        assertThat(result3)
                .isSuccessful()
                .hasResult(GuardrailResult.Result.SUCCESS);
    }
}
