package com.example.restservice.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WordTestController {

    // Matches the application destination prefix from your config (/app/test-word)
    @MessageMapping("/test-word")
    // Anyone subscribed to this topic will receive the broadcast
    @SendTo("/topic/test-greetings")
    public String handleTestWord(String wordContent) throws Exception {
        System.out.println("Backend caught submitted word: " + wordContent);
        return "Server confirmed: Someone just submitted the word \"" + wordContent + "\"!";
    }
}
