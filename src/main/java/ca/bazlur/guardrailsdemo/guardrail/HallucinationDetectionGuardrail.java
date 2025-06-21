package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.rag.AugmentationResult;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.HashSet;

@Component
public class HallucinationDetectionGuardrail implements OutputGuardrail {

  @Override
  public OutputGuardrailResult validate(OutputGuardrailRequest request) {
    var response = request.responseFromLLM();
    AugmentationResult augmentationResult = request.requestParams().augmentationResult();

    if (augmentationResult == null) {
      return success();
    }

    Set<String> responseFacts = extractKeyFacts(response.aiMessage().text());

    // Extract facts from the RAG context
    Set<String> contextFacts = new HashSet<>();
    augmentationResult.contents().forEach(content ->
        contextFacts.addAll(extractKeyFacts(content.textSegment().text()))
    );

    // Check if response facts are grounded in context
    long ungroundedFacts = responseFacts.stream()
        .filter(fact -> !isFactGrounded(fact, contextFacts))
        .count();

    if (ungroundedFacts > responseFacts.size() * 0.2) { // More than 20% ungrounded
      return reprompt(
          "Response contains potentially hallucinated information",
          "Please base your response only on the provided context. Do not add information that is not explicitly stated."
      );
    }

    return success();
  }

  private Set<String> extractKeyFacts(String text) {
    // Simple implementation - in production, use NLP techniques
    Set<String> facts = new HashSet<>();
    String[] sentences = text.split("[.!?]");
    for (String sentence : sentences) {
      if (sentence.trim().length() > 10) {
        facts.add(sentence.trim().toLowerCase());
      }
    }
    return facts;
  }

  private boolean isFactGrounded(String fact, Set<String> contextFacts) {
    // Simple similarity check - in production, use semantic similarity
    return contextFacts.stream()
        .anyMatch(contextFact -> contextFact.contains(fact) || fact.contains(contextFact));
  }
}