package com.example.restservice.service;

import com.example.restservice.dto.FlatLinkedWordDto;
import com.example.restservice.model.Word;
import com.example.restservice.repository.WordRepository;
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

    public WordService(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
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
    public Word createWord(Word word) {
        return wordRepository.save(word);
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
        if (!wordRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found");
        }
        wordRepository.deleteById(id);
    }
}
