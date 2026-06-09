package com.example.restservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.restservice.model.Page;
import com.example.restservice.model.Word;
import com.example.restservice.repository.PageRepository;
import com.example.restservice.repository.WordRepository;
import com.example.restservice.service.PageService;

@SpringBootTest
public class PageTests {
	@Autowired
	private PageService pageService;

    @Autowired
	private WordRepository wordRepository;

	@Autowired
	private PageRepository pageRepository;

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
		// 1. Arrange: Link the new test words straight to the end of the @BeforeEach chain (wordC)
		// Existing chain from setUp(): [wordA] -> [wordC]
		// New additions: -> [One] (3) -> [Two] (3) -> [Three] (5) -> [Four] (4) -> [Five] (4)
        Page giantPage = pageRepository.findAll().get(0); // Grab the page from setUp()
		Word w1 = wordRepository.save(new Word("One"));
		// Put everything onto one giant page to simulate a massive un-truncated state
        giantPage.setFirstWord(w1);
		Word w2 = wordRepository.save(new Word("Two"));
		Word w3 = wordRepository.save(new Word("Three"));
		Word w4 = wordRepository.save(new Word("Four"));
		Word w5 = wordRepository.save(new Word("Five"));

		// Build out the rest of the chain
		w1.setNextWord(w2); wordRepository.save(w1);
		w2.setNextWord(w3); wordRepository.save(w2);
		w3.setNextWord(w4); wordRepository.save(w3);
		w4.setNextWord(w5); wordRepository.save(w4);

		giantPage.setLastWord(w5);
		pageRepository.save(giantPage);

		// 2. Act: Run the global truncate with a 6-character limit
		pageService.globalTruncateAndRepaginate(9);

		// 3. Assert: Verify the exact page count and distribution across the database
		List<Page> allPages = pageRepository.findAll();
		assertEquals(3, allPages.size(), "The entire database should be sliced into exactly 5 pages");

		// Assert Page 1: ["One" -> "Two"] (3 + 1 + 3 = 7 chars)
		Page pOne = findPageStartingWith(allPages, w1);
		assertEquals(w2.getId(), pOne.getLastWord().getId());

		// Assert Page 2: ["Three" -> "Four"] (5 + 1 + 4 = 10 chars)
		Page pThree = findPageStartingWith(allPages, w3);
		assertEquals(w3.getId(), pThree.getLastWord().getId());

		// Assert Page 3: ["Four" -> "Five"] (4 + 1 + 4 = 9 chars)
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
