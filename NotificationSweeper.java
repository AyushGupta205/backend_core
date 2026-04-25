package com.virality.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationSweeper {
    
    private final NotificationService notificationService;
    
    public NotificationSweeper(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * CRON Sweeper: Runs every 5 minutes to sweep pending notifications.
     * In production, this would run every 15 minutes.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5 minutes in milliseconds
    public void sweepNotifications() {
        System.out.println("[CRON] Starting notification sweep...");
        notificationService.sweepPendingNotifications();
        System.out.println("[CRON] Notification sweep completed.");
    }
}
