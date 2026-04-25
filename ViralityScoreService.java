package com.virality.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ViralityScoreService {
    
    private final StringRedisTemplate redisTemplate;
    
    // Score points for different interaction types
    private static final long BOT_REPLY_POINTS = 1;
    private static final long HUMAN_LIKE_POINTS = 20;
    private static final long HUMAN_COMMENT_POINTS = 50;
    
    public ViralityScoreService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Updates the virality score for a post based on interaction type.
     * Thread-safe atomic INCR operation.
     */
    public void updateViralityScore(Long postId, InteractionType type) {
        String key = "post:" + postId + ":virality_score";
        long points = switch (type) {
            case BOT_REPLY -> BOT_REPLY_POINTS;
            case HUMAN_LIKE -> HUMAN_LIKE_POINTS;
            case HUMAN_COMMENT -> HUMAN_COMMENT_POINTS;
        };
        redisTemplate.opsForValue().increment(key, points);
    }
    
    /**
     * Gets the current virality score for a post.
     */
    public Long getViralityScore(Long postId) {
        String key = "post:" + postId + ":virality_score";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }
    
    public enum InteractionType {
        BOT_REPLY, HUMAN_LIKE, HUMAN_COMMENT
    }
}
