package com.example.restservice.controller;

import com.example.restservice.dto.ReactionUpdateEvent;
import com.example.restservice.enums.ReactionType;
import com.example.restservice.model.Author;
import com.example.restservice.model.Reaction;
import com.example.restservice.repository.AuthorRepository;
import com.example.restservice.repository.ReactionRepository;
import com.example.restservice.service.ReactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import java.util.Map;
import java.util.Optional;

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
            System.err.println("🚫 Rejected reaction: Unauthenticated author.");
            return;
        }

        // 1. Mid-session security ban verification
        String username = principal.getName();
        Author authorDetails = authorRepository.findAuthorByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Author not found"));

        if (!authorDetails.isEnabled()) {
            System.err.println("🚫 Mid-session rejection: Banned author '" + username + "' attempted a reaction.");
            return;
        }

        // 2. Extract payload variables safely
        Long wordId = Long.valueOf(String.valueOf(payload.get("wordId")));
        long currentPageId = Long.parseLong(String.valueOf(payload.get("currentPageId")));
        ReactionType incomingType = ReactionType.valueOf((String) payload.get("reactionType"));

        // 3. Process database toggle logic via your service layer
        // Modify your service logic to return a string action: "ADDED" or "REMOVED"
        String actionTaken = reactionService.handleReactionAndReturnAction(authorDetails.getId(), wordId, incomingType);

        // 4. Calculate fresh aggregate statistics
        long freshCount = reactionRepository.countByWordIdAndReactionType(wordId, incomingType);

        // 5. Package up the update event
        ReactionUpdateEvent event = new ReactionUpdateEvent(wordId, incomingType, freshCount, actionTaken);

        // 6. Broadcast the ripple to all authors viewing this page
        // Frontend subscribes to: /topic/page/{currentPageId}/reactions
        messagingTemplate.convertAndSend("/topic/page/" + currentPageId + "/reactions", event);

        System.out.println("❤️ Reaction update broadcasted for Word ID " + wordId + " on Page " + currentPageId);
    }
}