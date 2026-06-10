package com.example.restservice.controller;

import com.example.restservice.dto.FlatLinkedWordDto;
import com.example.restservice.model.Word;
import com.example.restservice.service.WordService;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origin}", allowCredentials = "true")
@RequestMapping(value = "/api/words")
@NullMarked
public class WordController {

    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
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
            @RequestParam(required = false) Long previousWordId
            
    ) throws InterruptedException {
        System.out.println(String.format("Word being created... %s %d", word, previousWordId));
        Thread.sleep(2500);
        return wordService.createWord(word, currentPageId, previousWordId);
    }

    @PutMapping("/{id}")
    public Word updateWord(@PathVariable Long id, @RequestBody Word wordDetails) {
        return wordService.updateWord(id, wordDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWord(@PathVariable Long id) {
        wordService.deleteWord(id);
        return ResponseEntity.noContent().build();
    }
}