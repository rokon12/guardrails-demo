package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.memory.ChatMemory;
import org.springframework.stereotype.Component;

@Component
public class ConversationContextGuardrail implements InputGuardrail {
  private static final int MAX_QUESTIONS_PER_TOPIC = 5;

  @Override
  public InputGuardrailResult validate(InputGuardrailRequest params) {
    ChatMemory memory = params.requestParams().chatMemory();

    if (memory == null || memory.messages().isEmpty()) {
      return success();
    }

    String currentQuestion = params.userMessage().singleText();

    for (ChatMessage message : memory.messages()) {
      getMessage(message);
    }

    long similarQuestions = memory.messages().stream()
        .filter(msg -> msg instanceof UserMessage)
        .map(msg -> getMessage(msg).toLowerCase())
        .filter(msgText -> currentQuestion.toLowerCase().contains(msgText) || 
                          msgText.contains(currentQuestion.toLowerCase()) ||
                          calculateSimilarity(msgText, currentQuestion.toLowerCase()) > 0.7)
        .count();
    if (similarQuestions > MAX_QUESTIONS_PER_TOPIC) {
      return failure("You've asked similar questions multiple times. Please try a different topic.");
    }

    return success();
  }

  private String getMessage(ChatMessage message) {
    return switch (message) {
      case UserMessage userMessage -> userMessage.singleText();
      case AiMessage aiMessage -> aiMessage.text();
      default -> message.toString();
    };
  }

  private double calculateSimilarity(String text1, String text2) {
    if (text1 == null || text2 == null) return 0.0;
    if (text1.equals(text2)) return 1.0;
    
    // Simple Levenshtein similarity
    int maxLen = Math.max(text1.length(), text2.length());
    if (maxLen == 0) return 1.0;
    
    return (maxLen - levenshteinDistance(text1, text2)) / (double) maxLen;
  }

  private int levenshteinDistance(String s1, String s2) {
    int[][] dp = new int[s1.length() + 1][s2.length() + 1];
    
    for (int i = 0; i <= s1.length(); i++) {
      for (int j = 0; j <= s2.length(); j++) {
        if (i == 0) {
          dp[i][j] = j;
        } else if (j == 0) {
          dp[i][j] = i;
        } else {
          dp[i][j] = Math.min(
              dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
              Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
          );
        }
      }
    }
    
    return dp[s1.length()][s2.length()];
  }
}
