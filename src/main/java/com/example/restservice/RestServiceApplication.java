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
import com.example.restservice.repository.CourseRepository;

@SpringBootApplication
public class RestServiceApplication implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CourseRepository courseRepository;

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
        Optional<Word> course = courseRepository.findById(10001L);
        logger.info("Course 10001 -> {}", course);

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
    Word newWord = new Word("yeet");
    
    // Capture the returned object from the server, which WILL have the generated ID
    Word savedWord = restClient.post()
              .uri("/api/words")
              .contentType(MediaType.APPLICATION_JSON)
              .body(newWord)
              .retrieve()
              .body(Word.class); // <-- Expect a Word object back

    if (savedWord != null && savedWord.getId() != null) {
        logger.info(savedWord.getId().toString());
    } else {
        logger.warn("Server saved the word but didn't return an ID.");
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
    }
} 