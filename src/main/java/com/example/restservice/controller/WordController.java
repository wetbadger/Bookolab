package com.example.restservice.controller;

import com.example.restservice.model.Word;
import com.example.restservice.repository.WordRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
// Spring Boot 4 natively supports the 'version' property for clean routing
@RequestMapping(value = "/api/words")
@NullMarked // Spring Boot 4 standard for compile-time null safety
public class WordController {

    private final WordRepository wordRepository;

    // Constructor injection is the recommended best practice
    public WordController(WordRepository repo) {
        this.wordRepository = repo;
    }

    // GET all words
    @GetMapping
    public List<Word> getAllCourses() {
        return wordRepository.findAll();
    }

    // GET a single word by ID
    @GetMapping("/{id}")
    public Word getWordById(@PathVariable Long id) {
        return wordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    // POST create a new word
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Word createCourse(@RequestBody Word word) {
        return wordRepository.save(word);
    }

    // PUT update an existing word
    @PutMapping("/{id}")
    public Word updateCourse(@PathVariable Long id, @RequestBody Word wordDetails) {
        return wordRepository.findById(id)
                .map(existingWord -> {
                    existingWord.setContent(wordDetails.getContent());
                    
                    // CRITICAL: Map the nextWord relationship as well
                    if (wordDetails.getNextWord() != null) {
                        existingWord.setNextWord(wordDetails.getNextWord());
                    }
                    
                    return wordRepository.save(existingWord);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
    }

    // DELETE a word
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        if (!wordRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found");
        }
        wordRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}