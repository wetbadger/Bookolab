package com.bookolab.restservice.service;

import com.bookolab.restservice.dto.CacheMetadata;
import com.bookolab.restservice.dto.LeaderboardUserDto;
import com.bookolab.restservice.repository.ReactionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final ReactionRepository reactionRepository;
    private static final int LEADERBOARD_LIMIT = 100; // Pulls top 100 users for the frontend to manage

    public LeaderboardService(ReactionRepository reactionRepository) {
        this.reactionRepository = reactionRepository;
    }

    /**
     * Fetches leaderboard data and caches it under the "leaderboard" cache key.
     */
    @Cacheable(value = "leaderboard", key = "'allUsers'")
    public List<LeaderboardUserDto> getLeaderboard() {
        List<Object[]> rawResults = reactionRepository.findTopUsersByReceivedReactions(LEADERBOARD_LIMIT);

        return rawResults.stream().map(result -> {
            Long id = (result[0] != null) ? ((Number) result[0]).longValue() : null;
            String username = (result[1] != null) ? (String) result[1] : "Anonymous";

            // Handle BigInteger/BigDecimal/Long conversions cleanly across various SQL dialects
            Long score = 0L;
            if (result[2] != null) {
                score = ((Number) result[2]).longValue();
            }

            return new LeaderboardUserDto(id, username, score);
        }).collect(Collectors.toList());
    }

    /**
     * Evicts the existing cache entries.
     */
    @CacheEvict(value = "leaderboard", allEntries = true)
    public void resetLeaderboard() {
        // Intentionally left blank. Spring AOP intercepts this and purges the cache.
    }

    /**
     * Re-triggers the cached method to fetch fresh metadata metrics for your Cron component.
     */
    public CacheMetadata getCacheMetadata() {
        List<LeaderboardUserDto> users = getLeaderboard();
        int totalUsers = users.size();
        int topUsersCount = Math.min(totalUsers, 10);
        return new CacheMetadata(totalUsers, topUsersCount);
    }
}