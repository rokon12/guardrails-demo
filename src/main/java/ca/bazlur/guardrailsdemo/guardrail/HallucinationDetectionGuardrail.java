package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.rag.AugmentationResult;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HallucinationDetectionGuardrail implements OutputGuardrail {

    private static final Set<String> UNCERTAINTY_MARKERS = Set.of(
            "might be", "could be", "possibly", "potentially", "may",
            "i think", "i believe", "it seems", "apparently", "probably"
    );

    @Override
    public OutputGuardrailResult validate(OutputGuardrailRequest request) {
        var response = request.responseFromLLM();
        String responseText = response.aiMessage().text();
        
        AugmentationResult augmentationResult = null;
        try {
            augmentationResult = request.requestParams().augmentationResult();
        } catch (Exception e) {
            log.debug("No augmentation result available, treating as context-free validation");
        }

        // Check for overly confident claims without context
        if (augmentationResult == null || augmentationResult.contents().isEmpty()) {
            return checkForUnsubstantiatedClaims(responseText);
        }

        Set<String> responseFacts = extractKeyFacts(responseText);

        // Extract facts from the RAG context
        Set<String> contextFacts = new HashSet<>();
        augmentationResult.contents().forEach(content ->
                contextFacts.addAll(extractKeyFacts(content.textSegment().text()))
        );

        // Check if response facts are grounded in context
        long ungroundedFacts = responseFacts.stream()
                .filter(fact -> !isFactGrounded(fact, contextFacts))
                .count();

        double ungroundedRatio = responseFacts.isEmpty() ? 0 : (double) ungroundedFacts / responseFacts.size();

        if (ungroundedRatio > 0.3) { // More than 30% ungrounded
            log.warn("High hallucination risk detected: {}% ungrounded facts", Math.round(ungroundedRatio * 100));
            return reprompt(
                    "Response contains potentially hallucinated information",
                    "Please base your response strictly on the provided context. Use phrases like 'According to the provided information...' when citing facts."
            );
        }

        return success();
    }

    private Set<String> extractKeyFacts(String text) {
        // Extract sentences that contain factual claims
        Set<String> facts = new HashSet<>();
        String[] sentences = text.split("[.!?]+");

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 10 && containsFactualClaim(trimmed)) {
                facts.add(trimmed.toLowerCase());
            }
        }
        return facts;
    }

    private boolean containsFactualClaim(String sentence) {
        String lower = sentence.toLowerCase();
        // Look for patterns that indicate factual claims
        return lower.matches(".*(is|are|was|were|has|have|had|will|can|does|did).*") ||
                lower.matches(".*\\b\\d+\\b.*") || // Contains numbers
                lower.matches(".*(fact|data|study|research|report|according to).*");
    }

    private boolean isFactGrounded(String fact, Set<String> contextFacts) {
        // Check if the fact has significant overlap with context
        return contextFacts.stream()
                .anyMatch(contextFact -> calculateSimilarity(fact, contextFact) > 0.6);
    }

    private double calculateSimilarity(String fact1, String fact2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(fact1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(fact2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private OutputGuardrailResult checkForUnsubstantiatedClaims(String text) {
        String lowerText = text.toLowerCase();

        // Check for absolute statements without uncertainty markers
        long absoluteStatements = Arrays.stream(text.split("[.!?]+"))
                .filter(s -> s.matches(".*(always|never|all|every|none|must|definitely|certainly|guaranteed).*"))
                .count();

        // Check if there are uncertainty markers to balance absolute claims
        boolean hasUncertaintyMarkers = UNCERTAINTY_MARKERS.stream()
                .anyMatch(lowerText::contains);

        if (absoluteStatements > 2 && !hasUncertaintyMarkers) {
            return reprompt(
                    "Response contains unsubstantiated absolute claims",
                    "Please avoid absolute statements unless you're certain. Use qualified language when appropriate."
            );
        }

        return success();
    }
}