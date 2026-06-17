package com.example.restservice.service;

import com.example.restservice.dto.BoundedPageResponse;
import com.example.restservice.dto.FlatLinkedWordDto;
import com.example.restservice.dto.PageResponseDto;
import com.example.restservice.dto.WordNodeDto;
import com.example.restservice.enums.ReactionType;
import com.example.restservice.model.Author;
import com.example.restservice.model.Page;
import com.example.restservice.model.Word;
import com.example.restservice.repository.AuthorRepository;
import com.example.restservice.repository.PageRepository;
import com.example.restservice.repository.ReactionRepository;
import com.example.restservice.repository.WordRepository;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@NullMarked
public class PageService {

    private final PageRepository pageRepository;
    private final WordRepository wordRepository;
    private final AuthorRepository authorRepository;
    private final ReactionRepository reactionRepository;

    public PageService(PageRepository pageRepository, WordRepository wordRepository, AuthorRepository authorRepository, ReactionRepository reactionRepository) {
        this.pageRepository = pageRepository;
        this.wordRepository = wordRepository;
        this.authorRepository = authorRepository;
        this.reactionRepository = reactionRepository;
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
        Long firstPreviousWordId = wordRepository.findByNextWord(first).map(Word::getId).orElse(null);
        Long lastNextId = (last != null && last.getNextWord() != null) ? last.getNextWord().getId() : null;
        Long lastPreviousWordId = wordRepository.findByNextWord(last).map(Word::getId).orElse(null);

        FlatLinkedWordDto firstWordDto = first != null ? new FlatLinkedWordDto(first.getId(), first.getContent(), firstNextId, firstPreviousWordId) : null;
        FlatLinkedWordDto lastWordDto = last != null ? new FlatLinkedWordDto(last.getId(), last.getContent(), lastNextId, lastPreviousWordId) : null;

        // 🚀 Add individual checks for the flat layout boundaries
        if (firstWordDto != null) {
            firstWordDto.setLikeCount(reactionRepository.countByWordIdAndReactionType(first.getId(), com.example.restservice.enums.ReactionType.LIKE));
            firstWordDto.setDislikeCount(reactionRepository.countByWordIdAndReactionType(first.getId(), com.example.restservice.enums.ReactionType.DISLIKE));
        }
        if (lastWordDto != null) {
            lastWordDto.setLikeCount(reactionRepository.countByWordIdAndReactionType(last.getId(), com.example.restservice.enums.ReactionType.LIKE));
            lastWordDto.setDislikeCount(reactionRepository.countByWordIdAndReactionType(last.getId(), com.example.restservice.enums.ReactionType.DISLIKE));
        }

        return new PageResponseDto(page.getId(), firstWordDto, lastWordDto);
    }

    /*
    The non-flat version of getPage will traverse the linked list until it reaches last word.
    This should only be called when initially loading the page for the user to limit server round-trips.
    Updates to the page will continuously happen on the front end whenever a user inserts or appends a word,
    and in these situations we want to use getFlatWordById().

    The user should be limited in their number of refreshes because the site could be DDOS'd by calling this
    function too many times. Or maybe the page should just be cached, so that refreshes don't call this
    function but goes to the cache instead. If the page has no changes, we simply return the cached version.
    */
    @Transactional(readOnly = true)
    public BoundedPageResponse getBoundedPage(Long id, String username) {
        Author author = null;
        if (username != null) {
            author = authorRepository.findAuthorByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }

        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));

        Word currentEntity = page.getFirstWord();
        Word lastEntity = page.getLastWord();
        Long lastWordIdOfPreviousPage = null;
        WordNodeDto headDto = null;
        FlatLinkedWordDto flatLastWord = null;

        if (currentEntity != null && lastEntity != null) {
            // 1. Create the head of our JSON tree
            String headAuthor = currentEntity.getAuthor() != null ? currentEntity.getAuthor().getUsername() : "Anonymous";
            headDto = new WordNodeDto(currentEntity.getId(), currentEntity.getContent(), headAuthor);
            WordNodeDto currentDto = headDto;

            // Keep a list of all Node DTOs we generate so we can enrich them in batch later
            List<WordNodeDto> allDtosOnPage = new java.util.ArrayList<>();
            allDtosOnPage.add(headDto);

            // 2. Loop through the linked list until we process the last word of THIS page
            while (currentEntity != null) {
                if (currentEntity.getId().equals(lastEntity.getId())) {
                    break;
                }

                currentEntity = currentEntity.getNextWord();

                if (currentEntity != null) {
                    String currentAuthor = currentEntity.getAuthor() != null ? currentEntity.getAuthor().getUsername() : "Anonymous";
                    WordNodeDto nextDto = new WordNodeDto(currentEntity.getId(), currentEntity.getContent(), currentAuthor);
                    currentDto.setNextWord(nextDto);
                    currentDto = nextDto;
                    allDtosOnPage.add(nextDto); // 👈 Track every node built
                }
            }


            // Batch-enrich all generated word DTO nodes with their reaction stats
            if (!allDtosOnPage.isEmpty()) {
                List<Long> wordIdsList = allDtosOnPage.stream().map(WordNodeDto::getId).toList();
                List<Map<String, Object>> rawCounts = reactionRepository.getReactionCountsForWords(wordIdsList);

                // Define lookups with wider scope so the loop can safely read them
                java.util.Set<Long> likedWordIdsSet = java.util.Collections.emptySet();
                java.util.Set<Long> dislikedWordIdsSet = java.util.Collections.emptySet();

                if (author != null) {
                    // Fetching the user's personal interactions
                    likedWordIdsSet = new java.util.HashSet<>(
                            reactionRepository.findWordIdsReactedByUser(author.getId(), wordIdsList, ReactionType.LIKE)
                    );
                    dislikedWordIdsSet = new java.util.HashSet<>(
                            reactionRepository.findWordIdsReactedByUser(author.getId(), wordIdsList, ReactionType.DISLIKE)
                    );
                }

                for (WordNodeDto dto : allDtosOnPage) {
                    long likes = rawCounts.stream()
                            .filter(m -> m.get("wordId").equals(dto.getId()) && m.get("type") == com.example.restservice.enums.ReactionType.LIKE)
                            .mapToLong(m -> (Long) m.get("cnt")).findFirst().orElse(0L);

                    long dislikes = rawCounts.stream()
                            .filter(m -> m.get("wordId").equals(dto.getId()) && m.get("type") == com.example.restservice.enums.ReactionType.DISLIKE)
                            .mapToLong(m -> (Long) m.get("cnt")).findFirst().orElse(0L);

                    dto.setLikeCount(likes);
                    dto.setDislikeCount(dislikes);

                    // Set the boolean states using O(1) set lookups
                    dto.setUserLiked(likedWordIdsSet.contains(dto.getId()));
                    dto.setUserDisliked(dislikedWordIdsSet.contains(dto.getId()));
                }
            }

            // 3. Map lastEntity directly to a flat DTO representation
            Long nextId = (lastEntity.getNextWord() != null) ? lastEntity.getNextWord().getId() : null;
            Long previousWordId = wordRepository.findByNextWord(currentEntity)
                    .map(Word::getId)
                    .orElse(null);
            flatLastWord = new FlatLinkedWordDto(lastEntity.getId(), lastEntity.getContent(), nextId, previousWordId);

            // Enforce counts on the standalone flatLastWord DTO as well
            if (flatLastWord != null) {
                flatLastWord.setLikeCount(reactionRepository.countByWordIdAndReactionType(lastEntity.getId(), com.example.restservice.enums.ReactionType.LIKE));
                flatLastWord.setDislikeCount(reactionRepository.countByWordIdAndReactionType(lastEntity.getId(), com.example.restservice.enums.ReactionType.DISLIKE));
            }
        }

        // 4. Get the last word id of the previous page.
        if (id > 1) {
            Page previousPage = pageRepository.findById(id - 1)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));

            Word lastWordOfPreviousPage = previousPage.getLastWord();

            // The previous page may be empty if someone deleted all the words on it
            // In that case, we look for the first populated previous page.
            int i = 2;
            while (lastWordOfPreviousPage == null && id - i > 0) {
                previousPage = pageRepository.findById(id - i)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));
                lastWordOfPreviousPage = previousPage.getLastWord();
                i += 1;
            }
            if (lastWordOfPreviousPage != null)
                lastWordIdOfPreviousPage = lastWordOfPreviousPage.getId();
        }

        return new BoundedPageResponse(page.getId(), headDto, flatLastWord, lastWordIdOfPreviousPage, pageRepository.count());
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

    /*
    Since pages are only added by the globalTruncateAndRepaginate function, there is
    no need to ever use this function. All pages are deleted at once and the word-list is repaginated.
    */
    @Transactional
    public void deletePage(Long id) {
        if (!pageRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
        }
        pageRepository.deleteById(id);
    }

    /*
    A cron task will run this function every 15 minutes. This will limit all pages to 1000 characters,
    either removing or adding pages to accomodate the added or deleted words.

    On the front end, users who are editing a word should be redirected to the page where their previous word is moved to.
    */
    @Transactional
    public void globalTruncateAndRepaginate(int maxCharacters) {
        // 1. Find the absolute head of the entire database text chain FIRST
        Word currentWord = wordRepository.findAll().stream()
                .filter(w -> wordRepository.findByNextWord(w).isEmpty())
                .findFirst()
                .orElse(null);

        if (currentWord == null) return; // DB is completely empty

        // 2. Wipe old pages and FORCE a flush to free up the unique database constraints immediately
        // pageRepository.deleteAll();
        // pageRepository.flush();
        pageRepository.truncateAndResetSequence();

        // 3. Setup traversal and page tracking variables
        Page currentPage = new Page();
        currentPage.setFirstWord(currentWord);
        int rollingCharacterCount = 0;
        Word previousWord = null;

        // 4. Step through every single character in the linked list
        while (currentWord != null) {
            int wordLength = currentWord.getContent().length();

            // Check if adding this word violates the maximum capacity of the current page
            if (rollingCharacterCount + wordLength > maxCharacters) {
                // Close out the filled page at the previous word anchor
                currentPage.setLastWord(previousWord != null ? previousWord : currentPage.getFirstWord());
                pageRepository.save(currentPage);

                // Spawn the next clean page block
                currentPage = new Page();
                currentPage.setFirstWord(currentWord);

                // RESET counter back to 0 because this is a brand new page
                rollingCharacterCount = 0;
            }

            // Ensure the current word's length is tracked,
            // whether it stayed on the old page or kicked off the new page.
            rollingCharacterCount += wordLength + 1; // Count space as a character.
            previousWord = currentWord;
            currentWord = currentWord.getNextWord();
        }

        // 5. Catch and save the final trailing page after exiting the loop
        if (currentPage.getFirstWord() != null) {
            currentPage.setLastWord(previousWord);
            pageRepository.save(currentPage);
        }
    }
}
