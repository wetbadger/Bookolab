package com.bookolab.restservice;

import com.bookolab.restservice.model.Author;
import com.bookolab.restservice.model.Page;
import com.bookolab.restservice.model.Word;
import com.bookolab.restservice.repository.AuthorRepository;
import com.bookolab.restservice.repository.PageRepository;
import com.bookolab.restservice.repository.WordRepository;
import com.bookolab.restservice.service.AuthorService;
import com.bookolab.restservice.service.WordService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorSerializationTests {

    @Autowired
    private WordService wordService;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthorService authorService;

    @Autowired
    private EntityManager entityManager;

    private Author testAuthor;

    private Word createTestWord(String content) {
        Word word = new Word(content, UUID.randomUUID().toString().substring(0, 8));
        return word;
    }

    @BeforeEach
    void setUp() {
        // Flush queued deletes before inserting: IDENTITY inserts bypass the
        // persistence context and would collide with not-yet-deleted rows.
        pageRepository.deleteAll();
        wordRepository.deleteAll();
        authorRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        testAuthor = authorRepository.save(new Author("Test", "$2a$10$STcpzOhXNfxwFEfLBXODIOaOeeISq5R5Ej62QsksYod.9eNLT6TpK"));

        Word wordA = wordRepository.save(createTestWord("WordA"));
        Page testPage = new Page();
        testPage.setFirstWord(wordA);
        testPage.setLastWord(wordA);
        pageRepository.save(testPage);

        wordA.setAuthor(testAuthor);
        wordRepository.save(wordA);
    }

    private void assertNoSensitiveFields(String json) {
        assertFalse(json.contains("password"), "JSON must not contain a password field");
        assertFalse(json.contains("$2a$"), "JSON must not contain a bcrypt hash");
        assertFalse(json.contains("authorities"), "JSON must not contain Spring Security authorities");
        assertFalse(json.contains("accountNonExpired"), "JSON must not contain UserDetails internals");
        assertFalse(json.contains("hibernateLazyInitializer"), "JSON must not contain Hibernate proxy noise");
    }

    @Test
    void testGetAllWords_SerializedAuthor_HasNoPassword() throws Exception {
        List<Word> words = wordService.getAllWords();

        String json = objectMapper.writeValueAsString(words);

        assertTrue(json.contains("Test"), "Author username should still be serialized");
        assertNoSensitiveFields(json);
    }

    @Test
    void testAllAuthors_SerializedAuthors_HaveNoPassword() throws Exception {
        List<Author> authors = authorService.allAuthors();

        String json = objectMapper.writeValueAsString(authors);

        assertTrue(json.contains("Test"), "Username should still be serialized");
        assertNoSensitiveFields(json);
    }
}
