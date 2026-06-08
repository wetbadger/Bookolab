package com.example.restservice.service;

import com.example.restservice.dto.BoundedPageResponse;
import com.example.restservice.dto.FlatLinkedWordDto;
import com.example.restservice.dto.PageResponseDto;
import com.example.restservice.dto.WordNodeDto;
import com.example.restservice.model.Page;
import com.example.restservice.model.Word;
import com.example.restservice.repository.PageRepository;
import com.example.restservice.repository.WordRepository;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@NullMarked
public class PageService {

    private final PageRepository pageRepository;
    private final WordRepository wordRepository;

    public PageService(PageRepository pageRepository, WordRepository wordRepository) {
        this.pageRepository = pageRepository;
        this.wordRepository = wordRepository;
    }

    @Transactional(readOnly = true)
    public List<Page> getAllPages() {
        return pageRepository.findAll();
    }

    /*
    The flat page returns a firstWord and lastWord with a nextWordId property rather than a next word entity.
    This is useful when there are updates to the page's first word or last word.
    Rather than loading the whole page at once, we get a new flat object of the page.
    The front end will still show its previous state and update the first word, last word, or both.
    */
    @Transactional(readOnly = true)
    public PageResponseDto getFlatPage(Long id) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));

        Word first = page.getFirstWord();
        Word last = page.getLastWord();

        Long firstNextId = (first != null && first.getNextWord() != null) ? first.getNextWord().getId() : null;
        Long firstPreviousWordId = wordRepository.findByNextWord(first)
                .map(Word::getId)
                .orElse(null); // Returns null if this word is the head of the list
        Long lastNextId = (last != null && last.getNextWord() != null) ? last.getNextWord().getId() : null;
        Long lastPreviousWordId = wordRepository.findByNextWord(last)
                .map(Word::getId)
                .orElse(null); // Returns null if this word is the head of the list

        FlatLinkedWordDto firstWordDto = first != null ? new FlatLinkedWordDto(first.getId(), first.getContent(), firstNextId, firstPreviousWordId) : null;
        FlatLinkedWordDto lastWordDto = last != null ? new FlatLinkedWordDto(last.getId(), last.getContent(), lastNextId, lastPreviousWordId) : null;

        return new PageResponseDto(page.getId(), firstWordDto, lastWordDto);
    }

    /*
    The non-flat version of getPage will traverse the linked list until it reaches last word.
    This should only be called when initially loading the page for the user to limit server round-trips.
    Updates to the page will continuously happen on the front end whenever a user inserts or appends a word,
    and in these situations we want to use getFlatWordById().
    */
    @Transactional(readOnly = true)
    public BoundedPageResponse getBoundedPage(Long id) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));

        Word currentEntity = page.getFirstWord();
        Word lastEntity = page.getLastWord();

        if (currentEntity == null || lastEntity == null) {
            return new BoundedPageResponse(page.getId(), null, null);
        }

        // 1. Create the head of our JSON tree
        WordNodeDto headDto = new WordNodeDto(currentEntity.getId(), currentEntity.getContent());
        WordNodeDto currentDto = headDto;

        // 2. Loop through the linked list until we process the last word of THIS page
        while (currentEntity != null) {
            if (currentEntity.getId().equals(lastEntity.getId())) {
                break; 
            }

            currentEntity = currentEntity.getNextWord();
            
            if (currentEntity != null) {
                WordNodeDto nextDto = new WordNodeDto(currentEntity.getId(), currentEntity.getContent());
                currentDto.setNextWord(nextDto);
                currentDto = nextDto;
            }
        }

        // 3. Map lastEntity directly to a flat DTO representation
        Long nextId = (lastEntity.getNextWord() != null) ? lastEntity.getNextWord().getId() : null;
        // Look up previous ID efficiently using the repository query
        Long previousWordId = wordRepository.findByNextWord(currentEntity)
                .map(Word::getId)
                .orElse(null); // Returns null if this word is the head of the list
        FlatLinkedWordDto flatLastWord = new FlatLinkedWordDto(lastEntity.getId(), lastEntity.getContent(), nextId, previousWordId);

        return new BoundedPageResponse(page.getId(), headDto, flatLastWord);
    }

    @Transactional
    public Page createPage(Page page) {
        return pageRepository.save(page);
    }

    @Transactional
    public Page updatePage(Long id, Page pageDetails) {
        return pageRepository.findById(id)
                .map(existingPage -> {
                    existingPage.setFirstWord(pageDetails.getFirstWord());
                    existingPage.setLastWord(pageDetails.getLastWord());
                    return pageRepository.save(existingPage);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found")); 
                // Note: Fixed an error in your original code where update/delete said "Course not found"
    }

    @Transactional
    public void deletePage(Long id) {
        if (!pageRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
        }
        pageRepository.deleteById(id);
    }
}
