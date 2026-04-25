package com.virality.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {
    
    private final StringRedisTemplate redisTemplate;
    
    private static final long NOTIFICATION_COOLDOWN_MINUTES = 15;
    
    public NotificationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Handle bot interaction notification for a user.
     * Implements smart batching with 15-minute cooldown.
     */
    public void handleBotInteraction(Long userId, String botName) {
        String cooldownKey = "user:" + userId + ":notif_cooldown";
        String pendingKey = "user:" + userId + ":pending_notifs";
        String notification = "Bot " + botName + " replied to your post";
        
        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);
        
        if (Boolean.TRUE.equals(hasCooldown)) {
            // User has been notified recently, queue this notification
            redisTemplate.opsForList().rightPush(pendingKey, notification);
        } else {
            // No recent notification, send immediate notification and set cooldown
            System.out.println("Push Notification Sent to User " + userId);
            redisTemplate.opsForValue().set(cooldownKey, "1", NOTIFICATION_COOLDOWN_MINUTES, TimeUnit.MINUTES);
        }
    }
    
    /**
     * Sweep pending notifications for all users.
     * Called by scheduled task.
     */
    public void sweepPendingNotifications() {
        Set<String> keys = redisTemplate.keys("user:*:pending_notifs");
        
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        for (String pendingKey : keys) {
            Long userId = extractUserId(pendingKey);
            if (userId == null) continue;
            
            // Get all pending notifications atomically
            List<String> notifications = redisTemplate.opsForList().range(pendingKey, 0, -1);
            
            if (notifications != null && !notifications.isEmpty()) {
                // Clear the list
                redisTemplate.delete(pendingKey);
                
                // Log summarized notification
                int count = notifications.size();
                String firstBot = extractBotName(notifications.get(0));
                System.out.println("Summarized Push Notification: Bot " + firstBot + " and [" + (count - 1) + "] others interacted with your posts for User " + userId);
            }
        }
    }
    
    /**
     * Get count of users with pending notifications (for monitoring).
     */
    public int getPendingNotificationUserCount() {
        Set<String> keys = redisTemplate.keys("user:*:pending_notifs");
        return keys != null ? keys.size() : 0;
    }
    
    private Long extractUserId(String key) {
        // key format: user:{id}:pending_notifs
        String[] parts = key.split(":");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private String extractBotName(String notification) {
        // notification format: "Bot {name} replied to your post"
        if (notification.startsWith("Bot ")) {
            int end = notification.indexOf(" replied");
            if (end > 4) {
                return notification.substring(4, end);
            }
        }
        return "Unknown";
    }
}
