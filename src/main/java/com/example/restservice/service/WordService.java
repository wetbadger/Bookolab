package com.example.restservice.service;

import com.example.restservice.dto.FlatLinkedWordDto;
import com.example.restservice.model.Word;
import com.example.restservice.model.Page;
import com.example.restservice.repository.PageRepository;
import com.example.restservice.repository.WordRepository;
import jakarta.annotation.Nullable;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@NullMarked
public class WordService {

    private final WordRepository wordRepository;
    private final PageRepository pageRepository;

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

    /**
     * Create a word.
     *
     * Inserts a word after the previous word.
     * If there is no previous word, assume we are at the beginning and change the first
     * page's first word to the word we are creating.
     *
     * @param newWord Description of the input parameter.
     * @param currentPage Used to decide if we should change the firtsWord or lastWord of the current page.
     * @param previousWordId Description of the input parameter.
     * @return the created word.
     */
    @Transactional
    public Word createWord(Word newWord, Long currentPageId, @Nullable Long previousWordId) {
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

            // Update firstWord or lastWord of current page
            // 1. Fetch the current page (this one should probably still throw if missing, assuming it's mandatory)
            Page currentPage = pageRepository.findById(currentPageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current page not found"));

            // 2. Use .orElse(null) so execution doesn't halt if these pages don't exist
            Page pageWithLastWord = pageRepository.findByLastWord(previousWord).orElse(null);
            Page pageWithFirstWord = pageRepository.findByFirstWord(savedWord.getNextWord()).orElse(null);

            // 3. Perform null-safe ID comparisons using Objects.equals()
            if (pageWithLastWord != null && Objects.equals(pageWithLastWord.getId(), currentPageId)) {
                currentPage.setLastWord(savedWord);
            }

            if (pageWithFirstWord != null && Objects.equals(pageWithFirstWord.getId(), currentPageId)) {
                currentPage.setFirstWord(savedWord);
            }

            // 4. Save your currentPage changes if necessary
            pageRepository.save(currentPage);

            return savedWord;
        }

        // If no previousWordId, it's a new head or a standalone word
        Page pageOne = pageRepository.findById(1L)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
        newWord.setNextWord(pageOne.getFirstWord());
        pageOne.setFirstWord(newWord);
        pageRepository.save(pageOne);
        return wordRepository.save(newWord);
    }

    /**
     * Create a series of words.
     *
     * Inserts a series of words after the previous word.
     * If there is no previous word, assume we are at the beginning and change the first
     * page's first word to the first word we are creating.
     *
     * @param newWords A list of words
     * @param currentPage Used to decide if we should change the firtsWord or lastWord of the current page.
     * @param previousWordId Description of the input parameter.
     * @return the created word.
     */
    @Transactional
    public Word[] createWords(String[] newWords, Long currentPageId, @Nullable Long previousWordId) {
        if (newWords == null || newWords.length == 0) {
            return new Word[0];
        }

        Word[] words = new Word[newWords.length];

        // 1. Instantly instantiate all Word entities in memory
        for (int i = 0; i < newWords.length; i++) {
            words[i] = new Word(newWords[i]);
        }

        // 2. Link the new words together to form a cohesive mini-chain
        for (int i = 0; i < words.length - 1; i++) {
            words[i].setNextWord(words[i + 1]);
        }

        // 3. Extract the first and last words of our new chain
        Word firstNewWord = words[0];
        Word lastNewWord = words[words.length - 1];

        // 4. Now use your single insert logic to graft the entire chain in at once!
        if (previousWordId != null) {
            Word previousWord = wordRepository.findById(previousWordId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Previous word not found"));

            // Cache the original next target anchor point
            Word nextWordAnchor = previousWord.getNextWord();

            // Sever the old link to satisfy database unique constraints
            previousWord.setNextWord(null);
            wordRepository.saveAndFlush(previousWord);

            // Point the tail of our new chain to the old anchor point
            lastNewWord.setNextWord(nextWordAnchor);
            
            // Save the new chain elements down to the database
            wordRepository.saveAll(Arrays.asList(words));
            wordRepository.flush();

            // Point our previous anchor to the head of our new chain
            previousWord.setNextWord(firstNewWord);
            wordRepository.save(previousWord);

            // Manage page boundary links safely using the last word of the new chain
            Page currentPage = pageRepository.findById(currentPageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current page not found"));

            Page pageWithLastWord = pageRepository.findByLastWord(previousWord).orElse(null);
            Page pageWithFirstWord = pageRepository.findByFirstWord(nextWordAnchor).orElse(null);

            if (pageWithLastWord != null && Objects.equals(pageWithLastWord.getId(), currentPageId)) {
                currentPage.setLastWord(lastNewWord); // The tail of our stream is now the page's last word
            }
            if (pageWithFirstWord != null && Objects.equals(pageWithFirstWord.getId(), currentPageId)) {
                currentPage.setFirstWord(firstNewWord); // The head of our stream is now the page's first word
            }
            pageRepository.save(currentPage);

        } else {
            // Handle insertion at the absolute front of page 1
            Page pageOne = pageRepository.findById(1L)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));
            
            lastNewWord.setNextWord(pageOne.getFirstWord());
            wordRepository.saveAll(Arrays.asList(words));
            wordRepository.flush();

            pageOne.setFirstWord(firstNewWord);
            pageRepository.save(pageOne);
        }

        return words;
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
