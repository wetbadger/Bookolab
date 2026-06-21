package com.bookolab.restservice;

import com.bookolab.restservice.model.Author;
import com.bookolab.restservice.model.Page;
import com.bookolab.restservice.model.Word;
import com.bookolab.restservice.repository.AuthorRepository;
import com.bookolab.restservice.repository.PageRepository;
import com.bookolab.restservice.repository.WordRepository;
import com.bookolab.restservice.service.WordService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WordTests {

    @Autowired
    private WordService wordService;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private AuthorRepository authorRepository;

    private Word wordA;
    private Word wordC;
    private Page testPage = new Page();
    private Author testAuthor = new Author("Test", "password");

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
        authorRepository.deleteAll();

        authorRepository.save(testAuthor);

        // Build a baseline linked list chain: Page -> [A] -> [C]
        wordA = wordRepository.save(createTestWord("WordA"));
        wordC = wordRepository.save(createTestWord("WordC"));

        wordA.setNextWord(wordC);
        wordA = wordRepository.save(wordA);

        // Assign a page bounding this chain
        testPage.setFirstWord(wordA);
        testPage.setLastWord(wordC);
        testPage = pageRepository.save(testPage);
    }

    @Test
    void testCreateWord_SpliceInMiddle_UpdatesPointersCorrectly() {
        // Arrange: Prepare a new word to inject between A and C
        Word wordB = createTestWord("WordB");

        // Act: Create WordB pointing to WordA as its previous anchor
        Word savedB = wordService.createWord(wordB, testPage.getId(), "abc123", wordA.getId(), null, testAuthor.getUsername());

        // Assert: Verify WordB stole WordA's old next pointer (WordC)
        assertNotNull(savedB.getId());
        assertNotNull(savedB.getNextWord());
        assertEquals(wordC.getId(), savedB.getNextWord().getId());

        // Assert: Verify WordA now links directly forward to WordB
        Word updatedA = wordRepository.findById(wordA.getId()).orElseThrow();
        assertNotNull(updatedA.getNextWord());
        assertEquals(savedB.getId(), updatedA.getNextWord().getId());
    }

    @Test
    void testCreateWord_NoPreviousId_CreatesIsolatedOrHeadNode() {
        // Act: Create a standalone word with no previous reference (Prepends to Page 1)
        Word standalone = wordService.createWord(createTestWord("Standalone"), testPage.getId(), "456def",  null, null, testAuthor.getUsername());

        // Assert: Verify it saved cleanly
        assertNotNull(standalone.getId());
        
        // Verify that it correctly pushed WordA down the line!
        assertNotNull(standalone.getNextWord());
        assertEquals(wordA.getId(), standalone.getNextWord().getId());
        
        // Dynamic look up instead of hardcoded 1L boundary tracking
        Page updatedPage = pageRepository.findById(testPage.getId()).orElseThrow();
        assertEquals(standalone.getId(), updatedPage.getFirstWord().getId());
    }

    @Test
    void testDeleteWord_MiddleNode_TightensChain() {
        // Arrange: Insert WordB to establish an [A] -> [B] -> [C] sequence
        Word wordB = wordService.createWord(createTestWord("WordB"), testPage.getId(), "789ghi", wordA.getId(), null, testAuthor.getUsername());

        // Act: Delete the middleman (WordB)
        testAuthor.setCreditsSpent(-100L);
        authorRepository.save(testAuthor);
        wordService.deleteWord(wordB.getId(), 1L, testAuthor.getUsername());

        // Assert: WordB should be completely deleted
        assertFalse(wordRepository.existsById(wordB.getId()));

        // Assert: The chain tightened back up. WordA must point straight to WordC
        Word updatedA = wordRepository.findById(wordA.getId()).orElseThrow();
        assertNotNull(updatedA.getNextWord());
        assertEquals(wordC.getId(), updatedA.getNextWord().getId());
    }

    @Test
    void testDeleteWord_FirstWordOfPage_ShiftsPageBoundaryForward() {
        // Act: Delete WordA, which is currently the 'firstWord' boundary of testPage
        testAuthor.setCreditsSpent(-100L);
        authorRepository.save(testAuthor);
        wordService.deleteWord(wordA.getId(), 1L, testAuthor.getUsername());

        // Assert: The page should automatically shift its head boundary to WordC
        Page updatedPage = pageRepository.findById(testPage.getId()).orElseThrow();
        assertNotNull(updatedPage.getFirstWord());
        assertEquals(wordC.getId(), updatedPage.getFirstWord().getId());
    }

    @Test
    void testDeleteWord_SingleWordPage_CollapsesPageBoundaries() {
        // Arrange: Set up an isolated page containing exactly ONE word
        Word loneWord = wordRepository.save(createTestWord("Lone"));
        Page singleWordPage = new Page();
        singleWordPage.setFirstWord(loneWord);
        singleWordPage.setLastWord(loneWord);
        singleWordPage = pageRepository.save(singleWordPage);

        // Act: Delete that single word
        testAuthor.setCreditsSpent(-100L);
        authorRepository.save(testAuthor);
        wordService.deleteWord(loneWord.getId(), singleWordPage.getId(), testAuthor.getUsername());

        // Assert: Page boundaries must fully collapse to null instead of holding broken pointers
        Page updatedPage = pageRepository.findById(singleWordPage.getId()).orElseThrow();
        assertNull(updatedPage.getFirstWord());
        assertNull(updatedPage.getLastWord());
    }
}