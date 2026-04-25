package com.virality.service;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class GuardrailService {
    
    private final StringRedisTemplate redisTemplate;
    
    // Guardrail limits
    private static final int MAX_BOT_REPLIES = 100;
    private static final int MAX_COMMENT_DEPTH = 20;
    private static final long COOLDOWN_MINUTES = 10;
    
    public GuardrailService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Horizontal Cap: Check if a post has reached the maximum bot replies limit.
     * Uses Redis INCR atomically to ensure thread-safe counting.
     * Returns true if allowed, throws 429 if limit exceeded.
     */
    public boolean checkHorizontalCap(Long postId, Long botId) {
        String key = "post:" + postId + ":bot_count";
        
        @SuppressWarnings({"rawtypes", "unchecked"})
        Long currentCount = (Long) redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForValue().increment(key);
                List results = operations.exec();
                return results.isEmpty() ? null : results.get(0);
            }
        });
        
        if (currentCount == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to acquire lock");
        }
        
        if (currentCount > MAX_BOT_REPLIES) {
            // Rollback: decrement since we're rejecting
            redisTemplate.opsForValue().decrement(key);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Horizontal cap exceeded: Post " + postId + " has reached " + MAX_BOT_REPLIES + " bot replies");
        }
        
        return true;
    }
    
    /**
     * Vertical Cap: Check if comment depth exceeds maximum allowed level.
     */
    public boolean checkVerticalCap(Integer depthLevel) {
        if (depthLevel > MAX_COMMENT_DEPTH) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Vertical cap exceeded: Comment depth " + depthLevel + " exceeds maximum of " + MAX_COMMENT_DEPTH);
        }
        return true;
    }
    
    /**
     * Cooldown Cap: Check if a bot has interacted with a specific human within cooldown period.
     * Uses Redis SET with TTL for atomic check-and-set operation.
     */
    public boolean checkCooldown(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Cooldown cap exceeded: Bot " + botId + " must wait " + ttl + " seconds before interacting with Human " + humanId);
        }
        
        // Set cooldown with TTL atomically
        redisTemplate.opsForValue().set(key, "1", COOLDOWN_MINUTES, TimeUnit.MINUTES);
        return true;
    }
    
    /**
     * Get current bot count for a post (useful for debugging/monitoring).
     */
    public Long getBotCount(Long postId) {
        String key = "post:" + postId + ":bot_count";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }
}
