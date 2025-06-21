package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class ContextAwareInputGuardrail implements InputGuardrail {
    
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final int MAX_SIMILAR_QUESTIONS = 3;
    private static final double SIMILARITY_THRESHOLD = 0.8;
    
    // Simple in-memory store for demonstration - in production use Redis or database
    private final ConcurrentHashMap<String, UserSession> userSessions = new ConcurrentHashMap<>();
    
    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String currentQuestion = userMessage.singleText();
        String sessionId = extractSessionId(userMessage); // This would come from context in real app
        
        UserSession session = userSessions.computeIfAbsent(sessionId, k -> new UserSession());
        
        // Rate limiting check
        if (isRateLimited(session)) {
            log.warn("Rate limit exceeded for session: {}", sessionId);
            return failure("You're sending messages too quickly. Please wait a moment before trying again.");
        }
        
        // Check for repetitive questions
        if (isRepetitiveQuestion(session, currentQuestion)) {
            log.info("Repetitive question detected for session: {}", sessionId);
            return failure("You've asked similar questions multiple times. Please try rephrasing your question or asking about a different topic.");
        }
        
        // Update session with current question
        session.addQuestion(currentQuestion);
        
        return success();
    }
    
    private boolean isRateLimited(UserSession session) {
        long currentTime = System.currentTimeMillis();
        long oneMinuteAgo = currentTime - 60_000; // 1 minute ago
        
        // Clean old requests
        session.requestTimes.removeIf(time -> time < oneMinuteAgo);
        
        // Check if exceeded rate limit
        if (session.requestTimes.size() >= MAX_REQUESTS_PER_MINUTE) {
            return true;
        }
        
        // Record current request
        session.requestTimes.add(currentTime);
        return false;
    }
    
    private boolean isRepetitiveQuestion(UserSession session, String currentQuestion) {
        long similarQuestions = session.recentQuestions.stream()
            .filter(q -> calculateSimilarity(q, currentQuestion) > SIMILARITY_THRESHOLD)
            .count();
        
        return similarQuestions >= MAX_SIMILAR_QUESTIONS;
    }
    
    private double calculateSimilarity(String s1, String s2) {
        // Simple Jaccard similarity - in production, use more sophisticated methods
        Set<String> set1 = new HashSet<>(Arrays.asList(s1.toLowerCase().split("\\s+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(s2.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }
    
    private String extractSessionId(UserMessage userMessage) {
        // In a real application, this would come from the request context
        // For demo purposes, we'll use a hash of the message
        return String.valueOf(Math.abs(userMessage.hashCode() % 1000));
    }
    
    private static class UserSession {
        private final Set<String> recentQuestions = ConcurrentHashMap.newKeySet();
        private final Set<Long> requestTimes = ConcurrentHashMap.newKeySet();
        private final AtomicInteger questionCount = new AtomicInteger(0);
        
        void addQuestion(String question) {
            // Keep only the last 10 questions to prevent memory issues
            if (questionCount.incrementAndGet() > 10) {
                recentQuestions.clear();
                questionCount.set(1);
            }
            recentQuestions.add(question);
        }
    }
}