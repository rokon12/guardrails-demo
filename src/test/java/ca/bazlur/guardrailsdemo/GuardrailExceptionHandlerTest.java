package ca.bazlur.guardrailsdemo;

import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GuardrailExceptionHandlerTest {

    private GuardrailExceptionHandler exceptionHandler;

    @Mock
    private WebRequest mockWebRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exceptionHandler = new GuardrailExceptionHandler();
        
        // Setup common mock behavior
        when(mockWebRequest.getDescription(false)).thenReturn("uri=/api/support/chat");
    }

    @Nested
    @DisplayName("Input Guardrail Exception Handling")
    class InputGuardrailExceptionHandling {

        @Test
        @DisplayName("Should handle input guardrail exception with proper error response")
        void shouldHandleInputGuardrailExceptionWithProperErrorResponse() {
            // Given
            String errorMessage = "Your message contains prohibited content";
            InputGuardrailException exception = new InputGuardrailException(errorMessage);

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleInputGuardrailException(exception, mockWebRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INPUT_VALIDATION_FAILED");
            assertThat(response.getBody().message()).isEqualTo("Your input did not pass validation");
            assertThat(response.getBody().details()).containsExactly(errorMessage);
            assertThat(response.getBody().timestamp()).isBefore(Instant.now().plusSeconds(1));
            assertThat(response.getBody().metadata()).containsKey("path");
            assertThat(response.getBody().metadata().get("path")).isEqualTo("/api/support/chat");
        }

        @Test
        @DisplayName("Should extract guardrail name from error message")
        void shouldExtractGuardrailNameFromErrorMessage() {
            // Given
            String errorMessage = "ContentSafety validation failed";
            InputGuardrailException exception = new InputGuardrailException(errorMessage);

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleInputGuardrailException(exception, mockWebRequest);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().metadata()).containsEntry("guardrail", "ContentSafetyInputGuardrail");
        }

        @Test
        @DisplayName("Should handle null error message gracefully")
        void shouldHandleNullErrorMessageGracefully() {
            // Given
            InputGuardrailException exception = new InputGuardrailException(null);

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleInputGuardrailException(exception, mockWebRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().details()).containsExactly("Input validation failed");
        }

        @Test
        @DisplayName("Should identify different guardrail types from messages")
        void shouldIdentifyDifferentGuardrailTypesFromMessages() {
            // Test cases for different guardrail types
            String[][] testCases = {
                {"PromptInjection detected", "PromptInjectionGuardrail"},
                {"ProfessionalTone issue", "ProfessionalToneOutputGuardrail"},
                {"Hallucination detected", "HallucinationDetectionGuardrail"},
                {"Unknown error", "Unknown"}
            };

            for (String[] testCase : testCases) {
                // Given
                InputGuardrailException exception = new InputGuardrailException(testCase[0]);

                // When
                ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                    exceptionHandler.handleInputGuardrailException(exception, mockWebRequest);

                // Then
                assertThat(response.getBody().metadata())
                    .as("Should identify guardrail type for message: %s", testCase[0])
                    .containsEntry("guardrail", testCase[1]);
            }
        }
    }

    @Nested
    @DisplayName("Output Guardrail Exception Handling")
    class OutputGuardrailExceptionHandling {

        @Test
        @DisplayName("Should handle output guardrail exception with proper error response")
        void shouldHandleOutputGuardrailExceptionWithProperErrorResponse() {
            // Given
            String errorMessage = "Response failed professional tone validation";
            OutputGuardrailException exception = new OutputGuardrailException(errorMessage);

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleOutputGuardrailException(exception, mockWebRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("OUTPUT_GENERATION_FAILED");
            assertThat(response.getBody().message()).isEqualTo("Unable to generate an appropriate response. Please try again later.");
            assertThat(response.getBody().details()).containsExactly("The AI response did not meet quality standards");
            assertThat(response.getBody().timestamp()).isBefore(Instant.now().plusSeconds(1));
            assertThat(response.getBody().metadata()).containsEntry("retries", "exhausted");
            assertThat(response.getBody().metadata()).containsEntry("path", "/api/support/chat");
        }

        @Test
        @DisplayName("Should handle null output error message")
        void shouldHandleNullOutputErrorMessage() {
            // Given
            OutputGuardrailException exception = new OutputGuardrailException(null);

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleOutputGuardrailException(exception, mockWebRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("OUTPUT_GENERATION_FAILED");
        }
    }

    @Nested
    @DisplayName("Generic Exception Handling")
    class GenericExceptionHandling {

        @Test
        @DisplayName("Should handle generic exceptions with internal server error")
        void shouldHandleGenericExceptionsWithInternalServerError() {
            // Given
            RuntimeException exception = new RuntimeException("Unexpected error occurred");

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleGenericException(exception, mockWebRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
            assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred. Please try again later.");
            assertThat(response.getBody().details()).isEmpty(); // No details in production mode
            assertThat(response.getBody().timestamp()).isBefore(Instant.now().plusSeconds(1));
            assertThat(response.getBody().metadata()).containsEntry("requestId", "unknown");
            assertThat(response.getBody().metadata()).containsEntry("path", "/api/support/chat");
        }

        @Test
        @DisplayName("Should handle null generic exception")
        void shouldHandleNullGenericException() {
            // Given
            RuntimeException exception = new RuntimeException((String) null);

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleGenericException(exception, mockWebRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        }

        @Test
        @DisplayName("Should handle different exception types")
        void shouldHandleDifferentExceptionTypes() {
            // Test different exception types
            Exception[] exceptions = {
                new IllegalArgumentException("Invalid argument"),
                new NullPointerException("Null pointer"),
                new IllegalStateException("Invalid state"),
                new RuntimeException("Runtime error")
            };

            for (Exception exception : exceptions) {
                // When
                ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                    exceptionHandler.handleGenericException(exception, mockWebRequest);

                // Then
                assertThat(response.getStatusCode())
                    .as("Should return 500 for exception type: %s", exception.getClass().getSimpleName())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                
                assertThat(response.getBody().code())
                    .as("Should return INTERNAL_ERROR code for exception type: %s", exception.getClass().getSimpleName())
                    .isEqualTo("INTERNAL_ERROR");
            }
        }
    }

    @Nested
    @DisplayName("Error Response Structure Tests")
    class ErrorResponseStructureTests {

        @Test
        @DisplayName("Should include all required fields in error response")
        void shouldIncludeAllRequiredFieldsInErrorResponse() {
            // Given
            InputGuardrailException exception = new InputGuardrailException("Test error");

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleInputGuardrailException(exception, mockWebRequest);

            // Then
            GuardrailExceptionHandler.ErrorResponse errorResponse = response.getBody();
            assertThat(errorResponse).isNotNull();
            assertThat(errorResponse.code()).isNotNull().isNotEmpty();
            assertThat(errorResponse.message()).isNotNull().isNotEmpty();
            assertThat(errorResponse.details()).isNotNull();
            assertThat(errorResponse.timestamp()).isNotNull();
            assertThat(errorResponse.metadata()).isNotNull();
        }

        @Test
        @DisplayName("Should support backward compatibility constructor")
        void shouldSupportBackwardCompatibilityConstructor() {
            // Given
            String code = "TEST_ERROR";
            String message = "Test error message";
            List<String> details = List.of("Detail 1", "Detail 2");

            // When
            GuardrailExceptionHandler.ErrorResponse errorResponse = 
                new GuardrailExceptionHandler.ErrorResponse(code, message, details);

            // Then
            assertThat(errorResponse.code()).isEqualTo(code);
            assertThat(errorResponse.message()).isEqualTo(message);
            assertThat(errorResponse.details()).isEqualTo(details);
            assertThat(errorResponse.timestamp()).isBefore(Instant.now().plusSeconds(1));
            assertThat(errorResponse.metadata()).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty details list")
        void shouldHandleEmptyDetailsList() {
            // Given
            OutputGuardrailException exception = new OutputGuardrailException("Output error");

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleOutputGuardrailException(exception, mockWebRequest);

            // Then
            assertThat(response.getBody().details()).isNotEmpty(); // Should have default message
        }
    }

    @Nested
    @DisplayName("Path Extraction Tests")
    class PathExtractionTests {

        @Test
        @DisplayName("Should extract path correctly from web request")
        void shouldExtractPathCorrectlyFromWebRequest() {
            // Given
            when(mockWebRequest.getDescription(false)).thenReturn("uri=/api/support/analyze");
            InputGuardrailException exception = new InputGuardrailException("Test error");

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleInputGuardrailException(exception, mockWebRequest);

            // Then
            assertThat(response.getBody().metadata())
                .containsEntry("path", "/api/support/analyze");
        }

        @Test
        @DisplayName("Should handle malformed URI description")
        void shouldHandleMalformedUriDescription() {
            // Given
            when(mockWebRequest.getDescription(false)).thenReturn("malformed-description");
            InputGuardrailException exception = new InputGuardrailException("Test error");

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleInputGuardrailException(exception, mockWebRequest);

            // Then
            assertThat(response.getBody().metadata())
                .containsEntry("path", "malformed-description");
        }

        @Test
        @DisplayName("Should handle empty URI description")
        void shouldHandleEmptyUriDescription() {
            // Given
            when(mockWebRequest.getDescription(false)).thenReturn("");
            InputGuardrailException exception = new InputGuardrailException("Test error");

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> response = 
                exceptionHandler.handleInputGuardrailException(exception, mockWebRequest);

            // Then
            assertThat(response.getBody().metadata())
                .containsEntry("path", "");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should maintain consistent response format across exception types")
        void shouldMaintainConsistentResponseFormatAcrossExceptionTypes() {
            // Given
            InputGuardrailException inputException = new InputGuardrailException("Input error");
            OutputGuardrailException outputException = new OutputGuardrailException("Output error");
            RuntimeException genericException = new RuntimeException("Generic error");

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> inputResponse = 
                exceptionHandler.handleInputGuardrailException(inputException, mockWebRequest);
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> outputResponse = 
                exceptionHandler.handleOutputGuardrailException(outputException, mockWebRequest);
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> genericResponse = 
                exceptionHandler.handleGenericException(genericException, mockWebRequest);

            // Then - All should have consistent structure
            assertResponseStructure(inputResponse.getBody());
            assertResponseStructure(outputResponse.getBody());
            assertResponseStructure(genericResponse.getBody());
        }

        @Test
        @DisplayName("Should provide appropriate HTTP status codes")
        void shouldProvideAppropriateHttpStatusCodes() {
            // Given
            InputGuardrailException inputException = new InputGuardrailException("Input error");
            OutputGuardrailException outputException = new OutputGuardrailException("Output error");
            RuntimeException genericException = new RuntimeException("Generic error");

            // When
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> inputResponse = 
                exceptionHandler.handleInputGuardrailException(inputException, mockWebRequest);
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> outputResponse = 
                exceptionHandler.handleOutputGuardrailException(outputException, mockWebRequest);
            ResponseEntity<GuardrailExceptionHandler.ErrorResponse> genericResponse = 
                exceptionHandler.handleGenericException(genericException, mockWebRequest);

            // Then
            assertThat(inputResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(outputResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(genericResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void assertResponseStructure(GuardrailExceptionHandler.ErrorResponse response) {
        assertThat(response).isNotNull();
        assertThat(response.code()).isNotNull().isNotEmpty();
        assertThat(response.message()).isNotNull().isNotEmpty();
        assertThat(response.details()).isNotNull();
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.metadata()).isNotNull();
    }
}