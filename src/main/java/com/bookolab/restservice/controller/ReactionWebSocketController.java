package com.bookolab.restservice.controller;

import com.bookolab.restservice.dto.ReactionUpdateEvent;
import com.bookolab.restservice.enums.ReactionType;
import com.bookolab.restservice.model.Author;
import com.bookolab.restservice.repository.AuthorRepository;
import com.bookolab.restservice.repository.ReactionRepository;
import com.bookolab.restservice.service.ReactionService;
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

    @MessageMapping("/send-reaction")
    public void handleReactionBroadcast(Map<String, Object> payload, java.security.Principal principal) {
        if (principal == null) {
            // System.err.println("🚫 Rejected reaction: Unauthenticated author.");
            return;
        }

        // 1. Mid-session security ban verification
        String username = principal.getName();
        Author authorDetails = authorRepository.findAuthorByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Author not found"));

        if (!authorDetails.isEnabled()) {
            // System.err.println("🚫 Mid-session rejection: Banned author '" + username + "' attempted a reaction.");
            return;
        }

        // 2. Extract payload variables safely
        Long wordId = Long.valueOf(String.valueOf(payload.get("wordId")));
        long currentPageId = Long.parseLong(String.valueOf(payload.get("currentPageId")));
        ReactionType incomingType = ReactionType.valueOf((String) payload.get("reactionType"));

        // 3. Process database toggle logic via your service layer
        String actionTaken = reactionService.handleReactionAndReturnAction(authorDetails.getId(), wordId, incomingType);
        // System.out.println("Action taken: " + actionTaken);

        String destination = "/topic/page/" + currentPageId + "/reactions";

        // 4. Handle Broadcast Logic based on Action
        if ("CHANGED".equals(actionTaken)) {
            // Find the opposite reaction type
            ReactionType previousType = (incomingType == ReactionType.LIKE) ? ReactionType.DISLIKE : ReactionType.LIKE;

            // Fetch fresh counts for BOTH types now that the DB has updated
            long freshIncomingCount = reactionRepository.countByWordIdAndReactionType(wordId, incomingType);
            long freshPreviousCount = reactionRepository.countByWordIdAndReactionType(wordId, previousType);

            // Broadcast 1: The old reaction was REMOVED (decremented)
            ReactionUpdateEvent removeEvent = new ReactionUpdateEvent(wordId, previousType, freshPreviousCount, "REMOVED");
            messagingTemplate.convertAndSend(destination, removeEvent);

            // Broadcast 2: The new reaction was ADDED (incremented)
            ReactionUpdateEvent addEvent = new ReactionUpdateEvent(wordId, incomingType, freshIncomingCount, "ADDED");
            messagingTemplate.convertAndSend(destination, addEvent);

            // System.out.println("🔄 Reaction CHANGED. Dual broadcast sent for Word ID " + wordId);

        } else if (!"REJECTED".equals(actionTaken)) {
            // Standard flow for "ADDED" or "REMOVED"
            long freshCount = reactionRepository.countByWordIdAndReactionType(wordId, incomingType);
            ReactionUpdateEvent event = new ReactionUpdateEvent(wordId, incomingType, freshCount, actionTaken);

            messagingTemplate.convertAndSend(destination, event);
            // System.out.println("❤️ Reaction update broadcasted for Word ID " + wordId + " on Page " + currentPageId);
        }
    }
}