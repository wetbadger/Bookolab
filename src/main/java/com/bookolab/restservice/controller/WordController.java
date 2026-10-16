package com.bookolab.restservice.controller;

import com.bookolab.restservice.dto.FlatLinkedWordDto;
import com.bookolab.restservice.model.Word;
import com.bookolab.restservice.service.PageService;
import com.bookolab.restservice.service.WordService;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origin}", allowCredentials = "true")
@RequestMapping(value = "/api/words")
@NullMarked
public class WordController {

    private final WordService wordService;
    private final PageService pageService;

    public WordController(WordService wordService, PageService pageService) {
        this.wordService = wordService;
        this.pageService = pageService;
    }

    @GetMapping
    public List<Word> getAllWords() {
        return wordService.getAllWords();
    }

    @GetMapping("/flat/{id}")
    public FlatLinkedWordDto getFlatWordById(@PathVariable Long id) {
        return wordService.getFlatWordById(id);
    }

    @GetMapping("/{id}")
    public Word getWordById(@PathVariable Long id) {
        return wordService.getWordById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Word createWord(
            @RequestBody Word word,
            @RequestParam(required = true) Long currentPageId,
            @RequestParam(required = true) String localId,
            @RequestParam(required = false) Long previousWordId,
            @RequestParam(required = false) String previousLocalId,
            Authentication authentication

    ) throws InterruptedException {
        // System.out.println(String.format("Word being created... content: %s localId: %s previousWordId: %d previousLocalId: %s", word, localId, previousWordId, previousLocalId));
        Thread.sleep(1000);
        return wordService.createWord(word, currentPageId, localId, previousWordId, previousLocalId, authentication.getName());
    }

    @PutMapping("/{id}")
    public Word updateWord(@PathVariable Long id, @RequestBody Word wordDetails, Authentication authentication) {
        return wordService.updateWord(id, wordDetails, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWord(@PathVariable Long id,
                                           @RequestParam(required = true) Long currentPageId,
                                           Authentication authentication) {
        wordService.deleteWord(id, currentPageId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{wordId}/page")
    public ResponseEntity<Map<String, Long>> getWordPageLocation(@PathVariable Long wordId) {
        // System.out.println("Finding word on page.");
        Long pageId = pageService.findWordPageLocation(wordId);
        // System.out.println("Word " + Long.toString(wordId) + " found on page: " + Long.toString(pageId));
        return ResponseEntity.ok(Map.of("pageId", pageId));
    }
}