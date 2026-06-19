package com.example.restservice;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import com.example.restservice.model.Word;
import com.example.restservice.model.Page;
import com.example.restservice.repository.WordRepository;
import com.example.restservice.repository.PageRepository;

@SpringBootApplication
@EnableScheduling
@EnableCaching
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
    public void run(String @NonNull ... args) throws Exception {
        logger.info("App started!");
    }
}
