package com.bookolab.restservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.bookolab.restservice.model.Page;
import com.bookolab.restservice.model.Word;
import com.bookolab.restservice.repository.PageRepository;
import com.bookolab.restservice.repository.WordRepository;
import com.bookolab.restservice.service.PageService;

@SpringBootTest
public class PageTests {
    @Autowired
    private PageService pageService;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private PageRepository pageRepository;

    // Helper method to instantiate words cleanly with required localId fields
    private Word createTestWord(String content) {
        Word word = new Word(content, "");
        word.setLocalId(UUID.randomUUID().toString().substring(0, 8));
        return word;
    }

    @BeforeEach
    void setUp() {
        // Clear repositories to ensure a pristine state for every test execution
        pageRepository.deleteAll();
        wordRepository.deleteAll();
        Page page = new Page();
        pageRepository.save(page);
    }
    
    @Test
    void testGlobalTruncateAndRepaginate_ProcessesEntireChainIntoBoundedPages() {
        // 1. Arrange: Link the new test words straight to the end of the @BeforeEach chain
        Page giantPage = pageRepository.findAll().get(0); // Grab the page from setUp()
        
        Word w1 = wordRepository.save(createTestWord("One"));
        // Put everything onto one giant page to simulate a massive un-truncated state
        giantPage.setFirstWord(w1);
        Word w2 = wordRepository.save(createTestWord("Two"));
        Word w3 = wordRepository.save(createTestWord("Three"));
        Word w4 = wordRepository.save(createTestWord("Four"));
        Word w5 = wordRepository.save(createTestWord("Five"));

        // Build out the rest of the chain
        w1.setNextWord(w2); wordRepository.save(w1);
        w2.setNextWord(w3); wordRepository.save(w2);
        w3.setNextWord(w4); wordRepository.save(w3);
        w4.setNextWord(w5); wordRepository.save(w4);

        giantPage.setLastWord(w5);
        pageRepository.save(giantPage);

        // 2. Act: Run the global truncate with a 9-character limit
        pageService.globalTruncateAndRepaginate(9);

        // 3. Assert: Verify the exact page count and distribution across the database
        List<Page> allPages = pageRepository.findAll();
        assertEquals(3, allPages.size(), "The entire database should be sliced into exactly 3 pages");

        // Assert Page 1: ["One" -> "Two"]
        Page pOne = findPageStartingWith(allPages, w1);
        assertEquals(w2.getId(), pOne.getLastWord().getId());

        // Assert Page 2: ["Three" -> "Four"]
        Page pThree = findPageStartingWith(allPages, w3);
        assertEquals(w3.getId(), pThree.getLastWord().getId());

        // Assert Page 3: ["Four" -> "Five"]
        Page pFour = findPageStartingWith(allPages, w4);
        assertEquals(w5.getId(), pFour.getLastWord().getId());
    }

    // Helper method to look up our newly mapped pages in the test assertion
    private Page findPageStartingWith(List<Page> pages, Word startWord) {
        return pages.stream()
                .filter(p -> p.getFirstWord() != null && p.getFirstWord().getId().equals(startWord.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not find a page starting with " + startWord.getContent()));
    }
}