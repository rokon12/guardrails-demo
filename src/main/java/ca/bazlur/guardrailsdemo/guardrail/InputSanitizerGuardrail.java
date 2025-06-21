package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class InputSanitizerGuardrail implements InputGuardrail {
  
  private static final Pattern DANGEROUS_CHARS = Pattern.compile("[<>{}\\[\\]|\\\\\"';()&%$#@!*+=~`]");
  private static final Pattern SQL_INJECTION = Pattern.compile("(?i)(union|select|insert|update|delete|drop|exec|script)", Pattern.CASE_INSENSITIVE);
  private static final Pattern XSS_PATTERNS = Pattern.compile("(?i)(<script|javascript:|vbscript:|onload=|onerror=)", Pattern.CASE_INSENSITIVE);

  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    String originalText = userMessage.singleText();
    String text = sanitizeInput(originalText);

    // Check if significant sanitization occurred
    if (text.length() < originalText.length() * 0.8) {
      return failure("Your message contained too many special characters and couldn't be processed safely.");
    }

    // Return success with the sanitized message
    return successWith(text);
  }

  private String sanitizeInput(String input) {
    if (input == null || input.trim().isEmpty()) {
      return input;
    }

    String sanitized = input;

    // Normalize Unicode characters to prevent encoding attacks
    sanitized = Normalizer.normalize(sanitized, Normalizer.Form.NFKC);

    // Remove null bytes and control characters (except newlines and tabs)
    sanitized = sanitized.replaceAll("[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]", "");

    // Remove excessive whitespace but preserve single spaces and newlines
    sanitized = sanitized.replaceAll("[ \\t]+", " ");
    sanitized = sanitized.replaceAll("\\n{3,}", "\\n\\n");

    // Remove potentially dangerous characters
    sanitized = DANGEROUS_CHARS.matcher(sanitized).replaceAll("");

    // Remove SQL injection patterns
    sanitized = SQL_INJECTION.matcher(sanitized).replaceAll("");

    // Remove XSS patterns
    sanitized = XSS_PATTERNS.matcher(sanitized).replaceAll("");

    // Smart truncation at sentence boundaries
    if (sanitized.length() > 800) {
      sanitized = truncateAtSentence(sanitized, 800);
    }

    return sanitized.trim();
  }

  private String truncateAtSentence(String text, int maxLength) {
    if (text.length() <= maxLength) {
      return text;
    }

    String truncated = text.substring(0, maxLength);
    
    // Find the last sentence boundary
    int lastPeriod = truncated.lastIndexOf('.');
    int lastQuestion = truncated.lastIndexOf('?');
    int lastExclamation = truncated.lastIndexOf('!');
    
    int lastSentenceBoundary = Math.max(Math.max(lastPeriod, lastQuestion), lastExclamation);
    
    if (lastSentenceBoundary > maxLength / 2) {
      return truncated.substring(0, lastSentenceBoundary + 1);
    }
    
    // If no good sentence boundary, truncate at word boundary
    int lastSpace = truncated.lastIndexOf(' ');
    if (lastSpace > maxLength / 2) {
      return truncated.substring(0, lastSpace) + "...";
    }
    
    return truncated + "...";
  }
}
