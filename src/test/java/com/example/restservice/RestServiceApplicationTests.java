package com.example.restservice.service;

import com.example.restservice.model.Page;
import com.example.restservice.model.Word;
import com.example.restservice.repository.PageRepository;
import com.example.restservice.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RestServiceApplicationTests {

	@Autowired
	private WordService wordService;

	@Autowired
	private PageService pageService;

	@Autowired
	private WordRepository wordRepository;

	@Autowired
	private PageRepository pageRepository;

    private Word wordA;
	private Word wordC;
	private Page testPage = new Page();

	@BeforeEach
	void setUp() {
		// Clear repositories to ensure a pristine state for every test execution
		pageRepository.deleteAll();
		wordRepository.deleteAll();

		// Build a baseline linked list chain: Page -> [A] -> [C]
		wordA = wordRepository.save(new Word("WordA"));
		wordC = wordRepository.save(new Word("WordC"));

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
		Word wordB = new Word("WordB");

		// Act: Create WordB pointing to WordA as its previous anchor
		Word savedB = wordService.createWord(wordB, wordA.getId());

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
		// Act: Create a standalone word with no previous reference
		Word standalone = wordService.createWord(new Word("Standalone"), null);

		// Assert: Ensure it saved cleanly without auto-linking to existing data
		assertNotNull(standalone.getId());
		assertNull(standalone.getNextWord());
	}

	@Test
	void testDeleteWord_MiddleNode_TightensChain() {
		// Arrange: Insert WordB to establish an [A] -> [B] -> [C] sequence
		Word wordB = wordService.createWord(new Word("WordB"), wordA.getId());

		// Act: Delete the middleman (WordB)
		wordService.deleteWord(wordB.getId());

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
		wordService.deleteWord(wordA.getId());

		// Assert: The page should automatically shift its head boundary to WordC
		Page updatedPage = pageRepository.findById(testPage.getId()).orElseThrow();
		assertNotNull(updatedPage.getFirstWord());
		assertEquals(wordC.getId(), updatedPage.getFirstWord().getId());
	}

	@Test
	void testDeleteWord_SingleWordPage_CollapsesPageBoundaries() {
		// Arrange: Set up an isolated page containing exactly ONE word
		Word loneWord = wordRepository.save(new Word("Lone"));
		Page singleWordPage = new Page();
		singleWordPage.setFirstWord(loneWord);
		singleWordPage.setLastWord(loneWord);
		singleWordPage = pageRepository.save(singleWordPage);

		// Act: Delete that single word
		wordService.deleteWord(loneWord.getId());

		// Assert: Page boundaries must fully collapse to null instead of holding broken pointers
		Page updatedPage = pageRepository.findById(singleWordPage.getId()).orElseThrow();
		assertNull(updatedPage.getFirstWord());
		assertNull(updatedPage.getLastWord());
	}

	@Test
	void testGlobalTruncateAndRepaginate_ProcessesEntireChainIntoBoundedPages() {
		// 1. Arrange: Link the new test words straight to the end of the @BeforeEach chain (wordC)
		// Existing chain from setUp(): [wordA] -> [wordC]
		// New additions: -> [One] (3) -> [Two] (3) -> [Three] (5) -> [Four] (4) -> [Five] (4)
		Word w1 = wordRepository.save(new Word("One"));
		Word w2 = wordRepository.save(new Word("Two"));
		Word w3 = wordRepository.save(new Word("Three"));
		Word w4 = wordRepository.save(new Word("Four"));
		Word w5 = wordRepository.save(new Word("Five"));

		// Connect the setup tail (wordC) to our first new test word (w1)
		wordC.setNextWord(w1);
		wordRepository.save(wordC);

		// Build out the rest of the chain
		w1.setNextWord(w2); wordRepository.save(w1);
		w2.setNextWord(w3); wordRepository.save(w2);
		w3.setNextWord(w4); wordRepository.save(w3);
		w4.setNextWord(w5); wordRepository.save(w4);

		// Put everything onto one giant page to simulate a massive un-truncated state
		Page giantPage = pageRepository.findAll().get(0); // Grab the page from setUp()
		giantPage.setLastWord(w5); // Extend its tail to the end of the new chain
		pageRepository.save(giantPage);

		// 2. Act: Run the global truncate with a 6-character limit
		pageService.globalTruncateAndRepaginate(6);

		// 3. Assert: Verify the exact page count and distribution across the database
		List<Page> allPages = pageRepository.findAll();
		assertEquals(6, allPages.size(), "The entire database should be sliced into exactly 6 pages");

		// Assert Page 1: ["WordA" -> "WordA"] (5 chars)
		Page pA = findPageStartingWith(allPages, wordA);
		assertEquals(wordA.getId(), pA.getLastWord().getId());

		// Assert Page 2: ["WordC" -> "WordC"] (5 chars)
		Page pC = findPageStartingWith(allPages, wordC);
		assertEquals(wordC.getId(), pC.getLastWord().getId());

		// Assert Page 3: ["One" -> "Two"] (3 + 3 = 6 chars)
		Page pOne = findPageStartingWith(allPages, w1);
		assertEquals(w2.getId(), pOne.getLastWord().getId());

		// Assert Page 4: ["Three" -> "Three"] (5 chars)
		Page pThree = findPageStartingWith(allPages, w3);
		assertEquals(w3.getId(), pThree.getLastWord().getId());

		// Assert Page 5: ["Four" -> "Four"] (4 chars)
		Page pFour = findPageStartingWith(allPages, w4);
		assertEquals(w4.getId(), pFour.getLastWord().getId());

		// Assert Page 6: ["Five" -> "Five"] (4 chars)
		Page pFive = findPageStartingWith(allPages, w5);
		assertEquals(w5.getId(), pFive.getLastWord().getId());
	}

	// Helper method to look up our newly mapped pages in the test assertion
	private Page findPageStartingWith(List<Page> pages, Word startWord) {
		return pages.stream()
				.filter(p -> p.getFirstWord() != null && p.getFirstWord().getId().equals(startWord.getId()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Could not find a page starting with " + startWord.getContent()));
	}
}