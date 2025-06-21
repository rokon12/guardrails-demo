package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ContentSafetyInputGuardrail implements InputGuardrail {
  private static final List<String> PROHIBITED_WORDS = List.of(
      "hack", "exploit", "bypass", "illegal", "fraud", "crack", "breach", 
      "penetrate", "malware", "virus", "trojan", "backdoor", "phishing",
      "spam", "scam", "steal", "theft", "identity", "password", "credential"
  );

  private static final List<Pattern> THREAT_PATTERNS = List.of(
      Pattern.compile("h[4@]ck", Pattern.CASE_INSENSITIVE),
      Pattern.compile("cr[4@]ck", Pattern.CASE_INSENSITIVE),
      Pattern.compile("expl[0o]it", Pattern.CASE_INSENSITIVE),
      Pattern.compile("byp[4@]ss", Pattern.CASE_INSENSITIVE),
      Pattern.compile("m[4@]lw[4@]re", Pattern.CASE_INSENSITIVE),
      Pattern.compile("[h4@][a@][c3][k4@]", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b[a-z]*[h4@][a@4]*[c3][k4@][a-z]*\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("[\\w\\s]*(?:how\\s+to|teach\\s+me|show\\s+me)\\s+(?:hack|exploit|bypass)", Pattern.CASE_INSENSITIVE)
  );

  private final int maxLength;

  public ContentSafetyInputGuardrail(@Value("${app.guardrails.input.max-length}") int maxLength) {
    this.maxLength = maxLength;
  }

  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    String originalText = userMessage.singleText();
    String text = originalText.toLowerCase();
    
    if (originalText.length() > maxLength) {
      return failure("Your message is too long. Please keep it under " + maxLength + " characters.");
    }
    if (text.isBlank()) {
      return failure("Your message cannot be empty.");
    }
    if (text.length() < 5) {
      return failure("Your message is too short. Please provide more details.");
    }

    // Check for prohibited words
    for (String word : PROHIBITED_WORDS) {
      if (text.contains(word)) {
        return failure("Your message contains prohibited content related to security threats.");
      }
    }

    // Check for obfuscated patterns
    for (Pattern pattern : THREAT_PATTERNS) {
      if (pattern.matcher(originalText).find()) {
        return failure("Your message contains potentially harmful content patterns.");
      }
    }

    // Check for suspicious character patterns
    if (containsSuspiciousObfuscation(originalText)) {
      return failure("Your message contains suspicious character substitutions.");
    }

    return success();
  }

  private boolean containsSuspiciousObfuscation(String text) {
    // Detect excessive special character substitution
    long specialChars = text.chars()
        .filter(c -> "@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) != -1)
        .count();
    
    // If more than 15% of characters are special symbols, it's suspicious
    return (double) specialChars / text.length() > 0.15;
  }
}
