package ca.bazlur.guardrailsdemo.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
public class RateLimitingGuardrail implements InputGuardrail {
    
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final int MAX_REQUESTS_PER_HOUR = 200;
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);
    
    private final ConcurrentHashMap<String, UserRateLimit> userLimits = new ConcurrentHashMap<>();
    private volatile Instant lastCleanup = Instant.now();
    
    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String userId = extractUserId(userMessage);
        Instant now = Instant.now();
        
        // Periodic cleanup of old entries
        if (Duration.between(lastCleanup, now).compareTo(CLEANUP_INTERVAL) > 0) {
            cleanupOldEntries(now);
            lastCleanup = now;
        }
        
        UserRateLimit userLimit = userLimits.computeIfAbsent(userId, k -> new UserRateLimit());
        
        // Clean old requests from this user
        userLimit.cleanOldRequests(now);
        
        // Check rate limits
        if (userLimit.getRequestsInLastMinute(now) >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for user {}: {} requests in last minute", userId, userLimit.getRequestsInLastMinute(now));
            return failure(String.format(
                "Rate limit exceeded. You can make up to %d requests per minute. Please wait before sending another message.",
                MAX_REQUESTS_PER_MINUTE
            ));
        }
        
        if (userLimit.getRequestsInLastHour(now) >= MAX_REQUESTS_PER_HOUR) {
            log.warn("Hourly rate limit exceeded for user {}: {} requests in last hour", userId, userLimit.getRequestsInLastHour(now));
            return failure(String.format(
                "Hourly rate limit exceeded. You can make up to %d requests per hour. Please try again later.",
                MAX_REQUESTS_PER_HOUR
            ));
        }
        
        // Record the request
        userLimit.addRequest(now);
        
        log.debug("Request recorded for user {}: {}/min, {}/hour", 
            userId, userLimit.getRequestsInLastMinute(now), userLimit.getRequestsInLastHour(now));
        
        return success();
    }
    
    private String extractUserId(UserMessage userMessage) {
        // In a real application, this would come from authentication context
        // For demo purposes, use a hash-based approach
        return "user_" + Math.abs(userMessage.hashCode() % 10000);
    }
    
    private void cleanupOldEntries(Instant now) {
        userLimits.entrySet().removeIf(entry -> {
            UserRateLimit limit = entry.getValue();
            limit.cleanOldRequests(now);
            // Remove users with no recent activity (last hour)
            return limit.isEmpty();
        });
        
        log.debug("Cleaned up rate limit entries. Active users: {}", userLimits.size());
    }
    
    private static class UserRateLimit {
        private final ConcurrentLinkedQueue<Instant> requests = new ConcurrentLinkedQueue<>();
        
        void addRequest(Instant timestamp) {
            requests.offer(timestamp);
        }
        
        void cleanOldRequests(Instant now) {
            Instant oneHourAgo = now.minus(Duration.ofHours(1));
            requests.removeIf(timestamp -> timestamp.isBefore(oneHourAgo));
        }
        
        int getRequestsInLastMinute(Instant now) {
            Instant oneMinuteAgo = now.minus(Duration.ofMinutes(1));
            return (int) requests.stream()
                .filter(timestamp -> timestamp.isAfter(oneMinuteAgo))
                .count();
        }
        
        int getRequestsInLastHour(Instant now) {
            return requests.size(); // Already cleaned to last hour
        }
        
        boolean isEmpty() {
            return requests.isEmpty();
        }
    }
}