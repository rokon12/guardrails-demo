package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.List;

@Component
public class PromptInjectionGuardrail implements InputGuardrail {
  private static final List<Pattern> INJECTION_PATTERNS = List.of(
      // Basic injection patterns
      Pattern.compile("ignore\\s+previous\\s+instructions", Pattern.CASE_INSENSITIVE),
      Pattern.compile("disregard\\s+all\\s+rules", Pattern.CASE_INSENSITIVE),
      Pattern.compile("you\\s+are\\s+now", Pattern.CASE_INSENSITIVE),
      Pattern.compile("new\\s+instructions:", Pattern.CASE_INSENSITIVE),
      
      // Obfuscated variations
      Pattern.compile("ign[0o]re\\s+[a@]ll\\s+previous", Pattern.CASE_INSENSITIVE),
      Pattern.compile("please\\s+ign[0o]re", Pattern.CASE_INSENSITIVE),
      Pattern.compile("f[0o]rget\\s+(?:everything|all|previous)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("disreg[4a]rd\\s+(?:all|everything)", Pattern.CASE_INSENSITIVE),
      
      // Role manipulation
      Pattern.compile("you\\s+are\\s+(?:now|actually|really)\\s+(?:a|an)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("act\\s+(?:as|like)\\s+(?:a|an)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("pretend\\s+(?:to\\s+be|you\\s+are)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("imagine\\s+you\\s+are", Pattern.CASE_INSENSITIVE),
      
      // System manipulation
      Pattern.compile("system\\s*[:;]", Pattern.CASE_INSENSITIVE),
      Pattern.compile("override\\s+(?:system|safety|security)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("bypass\\s+(?:safety|security|restrictions)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("disable\\s+(?:safety|security|filters)", Pattern.CASE_INSENSITIVE),
      
      // Instruction injection
      Pattern.compile("new\\s+(?:instructions|rules|guidelines)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("updated\\s+(?:instructions|rules|guidelines)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("revised\\s+(?:instructions|rules)", Pattern.CASE_INSENSITIVE),
      Pattern.compile("follow\\s+these\\s+(?:new|updated)", Pattern.CASE_INSENSITIVE),
      
      // End-of-prompt markers
      Pattern.compile("\\[\\s*/?\\s*(?:END|STOP|FINISH)\\s*\\]", Pattern.CASE_INSENSITIVE),
      Pattern.compile("---\\s*(?:END|STOP)\\s*---", Pattern.CASE_INSENSITIVE),
      
      // Multi-step injections
      Pattern.compile("step\\s+1[:\\s].*ignore", Pattern.CASE_INSENSITIVE),
      Pattern.compile("first[,\\s]+ignore", Pattern.CASE_INSENSITIVE),
      
      // Jailbreak attempts
      Pattern.compile("jailbreak", Pattern.CASE_INSENSITIVE),
      Pattern.compile("break\\s+out\\s+of", Pattern.CASE_INSENSITIVE),
      Pattern.compile("escape\\s+(?:your|the)\\s+(?:programming|constraints)", Pattern.CASE_INSENSITIVE)
  );

  private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
      // Character encoding attempts
      Pattern.compile("\\\\u[0-9a-fA-F]{4}", Pattern.CASE_INSENSITIVE),
      Pattern.compile("&#\\d+;", Pattern.CASE_INSENSITIVE),
      Pattern.compile("%[0-9a-fA-F]{2}", Pattern.CASE_INSENSITIVE),
      
      // Excessive punctuation (obfuscation)
      Pattern.compile("[!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>?/~`]{5,}"),
      
      // Repeated delimiters
      Pattern.compile("([-=_*#]{3,}|[.]{4,})")
  );

  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    String text = userMessage.singleText();
    
    // Check for direct injection attempts
    for (Pattern pattern : INJECTION_PATTERNS) {
      if (pattern.matcher(text).find()) {
        return fatal("Potential prompt injection detected. Please ask a genuine customer support question.");
      }
    }
    
    // Check for suspicious patterns (warnings, not blocks)
    int suspiciousCount = 0;
    for (Pattern pattern : SUSPICIOUS_PATTERNS) {
      if (pattern.matcher(text).find()) {
        suspiciousCount++;
      }
    }
    
    if (suspiciousCount >= 2) {
      return failure("Your message contains suspicious formatting. Please use plain text.");
    }
    
    // Additional heuristics
    if (containsExcessiveSpecialChars(text)) {
      return failure("Your message contains excessive special characters. Please simplify your question.");
    }
    
    if (containsMultipleLanguages(text)) {
      return failure("Please submit your question in a single language.");
    }

    return success();
  }

  private boolean containsExcessiveSpecialChars(String text) {
    long specialChars = text.chars()
        .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
        .count();
    
    return (double) specialChars / text.length() > 0.3;
  }

  private boolean containsMultipleLanguages(String text) {
    // Simple heuristic: check for mixed scripts
    boolean hasLatin = text.chars().anyMatch(c -> c >= 'A' && c <= 'z');
    boolean hasNonLatin = text.chars().anyMatch(c -> c > 127 && Character.isLetter(c));
    
    return hasLatin && hasNonLatin && text.length() > 20;
  }
}
