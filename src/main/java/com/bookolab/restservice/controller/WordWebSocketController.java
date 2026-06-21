package com.bookolab.restservice.controller;

import java.util.Map;
import java.util.Optional;

import com.bookolab.restservice.dto.FlatLinkedWordDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

import com.bookolab.restservice.dto.DeletionResult;
import com.bookolab.restservice.dto.WordNodeDto;
import com.bookolab.restservice.model.Page;
import com.bookolab.restservice.model.Word;
import com.bookolab.restservice.repository.AuthorRepository;
import com.bookolab.restservice.repository.PageRepository;
import com.bookolab.restservice.service.ReactionStatsService;
import com.bookolab.restservice.service.SlurDetector;
import com.bookolab.restservice.service.WordService;

@Controller
public class WordWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ReactionStatsService reactionStatsService;

    @Autowired
    private WordService wordService;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private SlurDetector slurDetector;

    @MessageMapping("/send-word")
    public void handleNewWordBroadcast(Map<String, Object> payload, java.security.Principal principal) throws InterruptedException {
        String username = validateAndGetUsername(principal, "an edit");

        String content = (String) payload.get("content");
        String localId = (String) payload.get("localId");
        long currentPageId = Long.parseLong(String.valueOf(payload.get("currentPageId")));

        if (slurDetector.isSlur(content)) {
            return;
        }

        Long previousWordId = payload.get("previousWordId") != null ?
                Long.valueOf(String.valueOf(payload.get("previousWordId"))) : null;
        String previousLocalId = (String) payload.get("previousLocalId");

        // 1. Process the core database insert
        Word transientWord = new Word(content, localId);
        Word savedDatabaseWord = wordService.createWord(transientWord, currentPageId, localId, previousWordId, previousLocalId, username);

        if (savedDatabaseWord.getLocalId().equals("dummy_word")) {
            return;
        }

        Long nextId = savedDatabaseWord.getNextWord() != null ? savedDatabaseWord.getNextWord().getId() : null;

        FlatLinkedWordDto dto = new FlatLinkedWordDto(
                savedDatabaseWord.getId(),
                localId,
                savedDatabaseWord.getContent(),
                nextId,
                previousWordId,
                savedDatabaseWord.getAuthor().getUsername()
        );

        Map<String, Object> wordAction = Map.of(
                "type", "CREATE_WORD",
                "word", dto
        );

        // 2. Broadcast the fresh word to the active page right away
        sendMessage(currentPageId, wordAction);
    }

    @MessageMapping("/delete-word")
    public void handleDeleteWordBroadcast(Map<String, Object> payload, java.security.Principal principal) {
        String username = validateAndGetUsername(principal, "a deletion");
        long wordId = Long.parseLong(String.valueOf(payload.get("wordId")));
        long currentPageId = Long.parseLong(String.valueOf(payload.get("currentPageId")));

        DeletionResult result = wordService.deleteWord(wordId, currentPageId, username);

        if (!result.getValid()) {
            // System.out.println("User be hacking.");
            return;
        }

        Long prevWordId = result.getPreviousWordId();
        WordNodeDto nextWord = result.getNextWord();

        Map<String, Object> deleteMessage = Map.of(
                "type", "DELETE_WORD",
                "previousWordId", Optional.ofNullable(prevWordId),
                "nextWord", Optional.ofNullable(nextWord)
        );

        // 1. Broadcast the deletion update to everyone on the current page
        sendMessage(currentPageId, deleteMessage);
    }

    private void sendMessage(Long currentPageId, Map<String, Object> wordAction) {
        messagingTemplate.convertAndSend("/topic/page/" + currentPageId, (Object) wordAction);

        // 3. REUSABLE CROSS-PAGE BOUNDARY DETECTION
        Page currentPage = pageRepository.findById(currentPageId).orElse(null);
        if (currentPage != null) {
            Long firstWordId = currentPage.getFirstWord() != null ? currentPage.getFirstWord().getId() : null;
            Long lastWordId = currentPage.getLastWord() != null ? currentPage.getLastWord().getId() : null;

            detectAndBroadcastBoundaryChanges(currentPageId, firstWordId, lastWordId);
        }
    }

    /**
     * Reusable engine to announce head and tail boundary modifications to adjacent pages
     */
    private void detectAndBroadcastBoundaryChanges(long currentPageId, Long currentFirstWordId, Long currentLastWordId) {
        long previousPageId = currentPageId - 1;
        long nextPageId = currentPageId + 1;

        Page previousPage = pageRepository.findById(previousPageId).orElse(null);
        Page nextPage = pageRepository.findById(nextPageId).orElse(null);

        while (previousPage != null && previousPage.getFirstWord() == null) {
            previousPageId--;
            previousPage = pageRepository.findById(previousPageId).orElse(null);
        }

        while (nextPage != null && nextPage.getFirstWord() == null) {
            nextPageId++;
            nextPage = pageRepository.findById(nextPageId).orElse(null);
        }

        if (previousPage != null)
            previousPageId = previousPage.getId();
        if (nextPage != null)
            nextPageId = nextPage.getId();

        // Notify previous page about head movement
        Map<String, Object> headBoundaryPatch = Map.of(
                "type", "NEXT_PAGE_HEAD_CHANGED",
                "newNextWordId", currentFirstWordId != null ? currentFirstWordId : "NULL"
        );
        messagingTemplate.convertAndSend("/topic/page/" + previousPageId, (Object) headBoundaryPatch);

        // Notify next page about tail movement
        Map<String, Object> tailBoundaryPatch = Map.of(
                "type", "PREVIOUS_PAGE_TAIL_CHANGED",
                "newLastWordIdOfPreviousPage", currentLastWordId != null ? currentLastWordId : "NULL"
        );
        messagingTemplate.convertAndSend("/topic/page/" + nextPageId, (Object) tailBoundaryPatch);

        // System.out.println("🔄 Boundary ripple updates sent for Page " + currentPageId);
    }

    private String validateAndGetUsername(java.security.Principal principal, String actionType) {
        if (principal == null) {
            // System.err.println("🚫 Rejected message: Unauthenticated user attempting " + actionType + ".");
            throw new org.springframework.security.access.AccessDeniedException("User is unauthenticated.");
        }

        String username = principal.getName();
        UserDetails userDetails = authorRepository.findAuthorByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!userDetails.isEnabled()) {
            // System.err.println("🚫 Mid-session rejection: Banned user '" + username + "' attempted " + actionType + ".");
            throw new org.springframework.security.access.AccessDeniedException("User account is disabled.");
        }

        return username;
    }
}