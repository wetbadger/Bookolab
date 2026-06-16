package com.example.restservice.controller;

import com.example.restservice.model.Word;
import com.example.restservice.repository.AuthorRepository;
import com.example.restservice.service.WordService;
import com.example.restservice.model.Page;
import com.example.restservice.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import java.util.Map;
import java.util.Optional;

@Controller
public class WordWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private WordService wordService;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @MessageMapping("/send-word")
    public void handleNewWordBroadcast(Map<String, Object> payload, java.security.Principal principal) throws InterruptedException {
        String username = validateAndGetUsername(principal, "an edit");

        String content = (String) payload.get("content");
        String localId = (String) payload.get("localId");
        long currentPageId = Long.parseLong(String.valueOf(payload.get("currentPageId")));
        
        Long previousWordId = payload.get("previousWordId") != null ? 
            Long.valueOf(String.valueOf(payload.get("previousWordId"))) : null;
        String previousLocalId = (String) payload.get("previousLocalId");

        // 1. Process the core database insert
        Word transientWord = new Word(content, localId);
        Word savedDatabaseWord = wordService.createWord(transientWord, currentPageId, localId, previousWordId, previousLocalId, username);
        
        // 2. CHANNEL 1: Broadcast the fresh word to the active page right away
        messagingTemplate.convertAndSend("/topic/page/" + currentPageId, savedDatabaseWord);

        // 3. CROSS-PAGE BOUNDARY DETECTION ENGINE
        // Fetch current page metadata boundaries to check if we modified the head or tail edges
        Page currentPage = pageRepository.findById(currentPageId).orElse(null);
        if (currentPage != null) {
            
            // CRITICAL CHECK A: Did we insert at the very beginning of this page?
            if (currentPage.getFirstWord() != null && currentPage.getFirstWord().getId().equals(savedDatabaseWord.getId())) {
                // If this page has a previous page, we must tell that previous page to update its nextWordId link
                long previousPageId = currentPageId - 1; // Assuming sequential sequential numerical IDs
                
                Map<String, Object> boundaryPatch = Map.of(
                    "type", "NEXT_PAGE_HEAD_CHANGED",
                    "newNextWordId", savedDatabaseWord.getId()
                );
                // Send to Channel 2 (The Previous Page Channel)
                messagingTemplate.convertAndSend("/topic/page/" + previousPageId, (Object) boundaryPatch);
                System.out.println("🔄 Sent head-patch boundary ripple back to Page " + previousPageId);
            }

            // CRITICAL CHECK B: Did we append to the very end of this page?
            if (currentPage.getLastWord() != null && currentPage.getLastWord().getId().equals(savedDatabaseWord.getId())) {
                long nextPageId = currentPageId + 1;
                
                Map<String, Object> boundaryPatch = Map.of(
                    "type", "PREVIOUS_PAGE_TAIL_CHANGED",
                    "newLastWordIdOfPreviousPage", savedDatabaseWord.getId()
                );
                // Send to Channel 3 (The Next Page Channel)
                messagingTemplate.convertAndSend("/topic/page/" + nextPageId, (Object) boundaryPatch);
                System.out.println("🔄 Sent tail-patch boundary ripple forward to Page " + nextPageId);
            }
        }
    }
    @MessageMapping("/delete-word")
    public void handleDeleteWordBroadcast(Map<String, Object> payload, java.security.Principal principal) {
        String username = validateAndGetUsername(principal, "a deletion");
        System.out.println(username);
        System.out.println(payload);
    }

    private String validateAndGetUsername(java.security.Principal principal, String actionType) {
        if (principal == null) {
            System.err.println("🚫 Rejected message: Unauthenticated user attempting " + actionType + ".");
            throw new org.springframework.security.access.AccessDeniedException("User is unauthenticated.");
        }

        String username = principal.getName();
        UserDetails userDetails = authorRepository.findAuthorByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!userDetails.isEnabled()) {
            System.err.println("🚫 Mid-session rejection: Banned user '" + username + "' attempted " + actionType + ".");
            throw new org.springframework.security.access.AccessDeniedException("User account is disabled.");
        }

        return username;
    }
}