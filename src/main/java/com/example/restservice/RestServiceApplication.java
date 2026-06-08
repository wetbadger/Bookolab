package com.example.restservice;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import com.example.restservice.model.Word;
import com.example.restservice.model.Page;
import com.example.restservice.repository.WordRepository;
import com.example.restservice.repository.PageRepository;

@SpringBootApplication
public class RestServiceApplication implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private WordRepository wordRepository;
    @Autowired
    private PageRepository pageRepository;

    // 1. Define the missing bean so Spring can find it anywhere in your app
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    public static void main(String[] args) {
        SpringApplication.run(RestServiceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Querying the DB directly works fine at startup
        Optional<Word> firstWord = wordRepository.findById(10001L);
        Optional<Page> pageOne = pageRepository.findById(1L);
        logger.info("Course 10001 -> {}", firstWord);

        // 2. Build the RestClient locally using the bean we defined above
        RestClient restClient = restClientBuilder().baseUrl("http://localhost:8080").build();

        try {
            String response = restClient.get()
                    .uri("/api/words/10001")
                    .retrieve()
                    .body(String.class);

            logger.info("API Response: " + response);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        try {
            Word previousWord = firstWord.orElseThrow(() -> new RuntimeException("Course 10001 not found"));
            for (int i = 1; i <= 100; i++) {
                Word newWord = new Word("yeet" + Integer.toString(i));
                
                // Capture the returned object from the server, which WILL have the generated ID
                Word savedWord = restClient.post()
                        .uri("/api/words")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(newWord)
                        .retrieve()
                        .body(Word.class); // <-- Expect a Word object back

                logger.info(String.format("Setting %s next word to %s", previousWord.getContent(), savedWord.getContent()));
                previousWord.setNextWord(savedWord);
                
                // Capture the Page object returned from the server
                /*
                Page pageOne = restClient.get()
                        .uri("/api/pages/{id}", 1L) // Pass the ID as a path variable
                        .accept(MediaType.APPLICATION_JSON) // Tell the server you expect JSON back
                        .retrieve()
                        .body(Page.class); // <-- Expect a Page object back
                */

                pageOne.ifPresent(x -> x.setLastWord(savedWord));

                // This will now send the properly formed Word object JSON
                Word savedPreviousWord = restClient.put()
                        .uri("/api/words/"+Long.toString(previousWord.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(previousWord) // <-- Fixed: Passing the raw Word object
                        .retrieve()
                        .body(Word.class);

                // 1. Properly pull the Page object out of the Optional wrapper
                Page pageToUpdate = pageOne.orElseThrow(() -> new RuntimeException("Page 1 not found"));

                // 2. Pass the unwrapped 'pageToUpdate' object to the request body
                Page updatedPage = restClient.put()
                        .uri("/api/pages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(pageToUpdate) // <-- Fixed: Passing the unwrapped Page object
                        .retrieve()
                        .body(Page.class);

                if (savedWord != null && savedWord.getId() != null) {
                    logger.info(savedWord.getId().toString());
                } else {
                    logger.warn("Server saved the word but didn't return an ID.");
                }
                if (savedPreviousWord != null && savedPreviousWord.getId() != null) {
                    logger.info(savedPreviousWord.getId().toString());
                } else {
                    logger.warn("Server saved the word but didn't return an ID.");
                }
                if (updatedPage != null && updatedPage.getId() != null) {
                    logger.info(updatedPage.getId().toString());
                } else {
                    logger.warn("Server saved the page but didn't return an ID.");
                }
                previousWord = savedWord;
            }
        } catch (Exception e) {
            logger.error("Error during POST request: ", e);
        }

        try {
            String response = restClient.get()
                    .uri("/api/words/1")
                    .retrieve()
                    .body(String.class);

            logger.info("API Response: " + response);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        try {
            String response = restClient.get()
                    .uri("/api/pages/1")
                    .retrieve()
                    .body(String.class);

            logger.info("API Response: " + response);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
