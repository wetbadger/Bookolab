package com.bookolab.restservice.controller;

import com.bookolab.restservice.dto.ReactionUpdateEvent;
import com.bookolab.restservice.dto.UserReactionStats;
import com.bookolab.restservice.enums.ReactionType;
import com.bookolab.restservice.model.Author;
import com.bookolab.restservice.repository.AuthorRepository;
import com.bookolab.restservice.repository.ReactionRepository;
import com.bookolab.restservice.service.ReactionService;
import com.bookolab.restservice.service.ReactionStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class ReactionWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ReactionStatsService reactionStatsService; // Inject the new service

    @MessageMapping("/send-reaction")
    public void handleReactionBroadcast(Map<String, Object> payload, java.security.Principal principal) {
        if (principal == null) {
            return;
        }

        String username = principal.getName();
        Author authorDetails = authorRepository.findAuthorByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Author not found"));

        if (!authorDetails.isEnabled()) {
            return;
        }

        Long wordId = Long.valueOf(String.valueOf(payload.get("wordId")));
        long currentPageId = Long.parseLong(String.valueOf(payload.get("currentPageId")));
        ReactionType incomingType = ReactionType.valueOf((String) payload.get("reactionType"));

        String actionTaken = reactionService.handleReactionAndReturnAction(authorDetails.getId(), wordId, incomingType);

        String pageDestination = "/topic/page/" + currentPageId + "/reactions";

        if ("CHANGED".equals(actionTaken)) {
            ReactionType previousType = (incomingType == ReactionType.LIKE) ? ReactionType.DISLIKE : ReactionType.LIKE;

            long freshIncomingCount = reactionRepository.countByWordIdAndReactionType(wordId, incomingType);
            long freshPreviousCount = reactionRepository.countByWordIdAndReactionType(wordId, previousType);

            ReactionUpdateEvent removeEvent = new ReactionUpdateEvent(wordId, previousType, freshPreviousCount, "REMOVED");
            messagingTemplate.convertAndSend(pageDestination, removeEvent);

            ReactionUpdateEvent addEvent = new ReactionUpdateEvent(wordId, incomingType, freshIncomingCount, "ADDED");
            messagingTemplate.convertAndSend(pageDestination, addEvent);

        } else if (!"REJECTED".equals(actionTaken)) {
            long freshCount = reactionRepository.countByWordIdAndReactionType(wordId, incomingType);
            ReactionUpdateEvent event = new ReactionUpdateEvent(wordId, incomingType, freshCount, actionTaken);
            messagingTemplate.convertAndSend(pageDestination, event);
        }

        // 5. Broadcast user reaction stats to the AUTHOR of the word
        // This now uses the service with proper transaction management
        reactionStatsService.broadcastWordAuthorReactionStats(wordId);
    }

    @MessageMapping("/get-my-reaction-stats")
    public void getMyReactionStats(java.security.Principal principal) {
        if (principal == null) return;

        try {
            UserReactionStats stats = reactionStatsService.getUserReactionStats(principal.getName());
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/reaction-stats",
                    stats
            );
        } catch (Exception e) {
            System.err.println("❌ Failed to get user reaction stats: " + e.getMessage());
        }
    }
}