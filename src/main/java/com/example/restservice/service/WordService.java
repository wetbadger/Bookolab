package com.example.restservice.service;

import com.example.restservice.dto.FlatLinkedWordDto;
import com.example.restservice.model.Word;
import com.example.restservice.repository.PageRepository;
import com.example.restservice.repository.WordRepository;
import jakarta.annotation.Nullable;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@NullMarked
public class WordService {

    private final WordRepository wordRepository;
    private final PageRepository pageRepository; // Only used for deletion edge case

    // Update constructor injection
    public WordService(WordRepository wordRepository, PageRepository pageRepository) {
        this.wordRepository = wordRepository;
        this.pageRepository = pageRepository;
    }

    @Transactional(readOnly = true)
    public List<Word> getAllWords() {
        return wordRepository.findAll();
    }

    /*
    This should always be used instead of getWordById.
    When a word object needs to be retrieved, we need to show a flat word object with the nextWordId property.
    */
    @Transactional(readOnly = true)
    public FlatLinkedWordDto getFlatWordById(Long id) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
        
        // Find next ID safely
        Long nextWordId = (word.getNextWord() != null) ? word.getNextWord().getId() : null;
        
        // Look up previous ID efficiently using the repository query
        Long previousWordId = wordRepository.findByNextWord(word)
                .map(Word::getId)
                .orElse(null); // Returns null if this word is the head of the list
        
        return new FlatLinkedWordDto(word.getId(), word.getContent(), nextWordId, previousWordId);
    }

    /*
    This should not be called from the front end as it will recursively return all words in the database.
    */
    @Transactional(readOnly = true)
    public Word getWordById(Long id) {
        return wordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
    }

    @Transactional
    public Word createWord(Word newWord, @Nullable Long previousWordId) {
        if (previousWordId != null) {
            // 1. Find the word we are attaching to
            Word previousWord = wordRepository.findById(previousWordId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Previous word not found"));

            // 2. Cache the next word link so we don't lose it
            Word nextWordAnchor = previousWord.getNextWord();

            // 3. SEVER the link on the previous word immediately to free up the unique constraint
            previousWord.setNextWord(null);
            wordRepository.saveAndFlush(previousWord); // Force the DB to see next_word_id is now NULL

            // 4. Give the cached link to the new word
            newWord.setNextWord(nextWordAnchor);
            Word savedWord = wordRepository.saveAndFlush(newWord); // Save newWord safely

            // 5. Point the previous word to the newly saved word
            previousWord.setNextWord(savedWord);
            wordRepository.save(previousWord);

            return savedWord;
        }

        // If no previousWordId, it's a new head or a standalone word
        return wordRepository.save(newWord);
    }

    @Transactional
    public Word updateWord(Long id, Word wordDetails) {
        return wordRepository.findById(id)
                .map(existingWord -> {
                    existingWord.setContent(wordDetails.getContent());
                    
                    // Map the nextWord relationship safely
                    if (wordDetails.getNextWord() != null) {
                        existingWord.setNextWord(wordDetails.getNextWord());
                    }
                    
                    return wordRepository.save(existingWord);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
    }

    @Transactional
    public void deleteWord(Long id) {
        Word wordToDelete = wordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));

        Word nextWord = wordToDelete.getNextWord();
        Word previousWord = wordRepository.findByNextWord(wordToDelete).orElse(null);

        patchPageBoundaries(wordToDelete, previousWord, nextWord);

        if (previousWord != null) {
            previousWord.setNextWord(nextWord);
            wordRepository.save(previousWord);
        }

        wordToDelete.setNextWord(null);
        wordRepository.saveAndFlush(wordToDelete);
        wordRepository.delete(wordToDelete);
    }

    private void patchPageBoundaries(Word wordToDelete, @Nullable Word previousWord, @Nullable Word nextWord) {
        // Fix page where this was the first word
        pageRepository.findByFirstWord(wordToDelete).ifPresent(page -> {
            if (wordToDelete.equals(page.getLastWord())) {
                page.setFirstWord(null);
                page.setLastWord(null);
            } else {
                page.setFirstWord(nextWord);
            }
            pageRepository.save(page);
        });

        // Fix page where this was the last word
        pageRepository.findByLastWord(wordToDelete).ifPresent(page -> {
            page.setLastWord(previousWord);
            pageRepository.save(page);
        });
    }
}
