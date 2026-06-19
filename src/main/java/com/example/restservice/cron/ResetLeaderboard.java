// ResetLeaderboard.java (Updated)
package com.example.restservice.cron;

import com.example.restservice.service.LeaderboardService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ResetLeaderboard {

    private final LeaderboardService leaderboardService;

    public ResetLeaderboard(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * Execute every minute to refresh the leaderboard cache
     * Cron pattern: second minute hour day month day-of-week
     */
    @Scheduled(cron = "0 * * * * *") // Every minute at second 0
    public void executeTask() {
        try {
            LocalDateTime now = LocalDateTime.now();
            String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // System.out.printf("🔄 [%s] Starting leaderboard cache refresh...%n", timestamp);

            // Refresh the cache
            leaderboardService.resetLeaderboard();

            // Get cache metadata
            var metadata = leaderboardService.getCacheMetadata();
            //System.out.printf("✅ [%s] Cache updated successfully. Users: %d, Top 10: %d%n",
            //        timestamp, metadata.totalUsers(), metadata.topUsersCount());

        } catch (Exception e) {
            System.err.printf("❌ [%s] Cron task failed: %s%n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    e.getMessage());
            e.printStackTrace();
        }
    }
}