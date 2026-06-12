package com.example.restservice.controller;

import com.example.restservice.model.Word;
import com.example.restservice.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class WordWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private WordService wordService; 

    @MessageMapping("/test-word")
    public void handleNewWordBroadcast(Map<String, Object> payload) throws InterruptedException {
        // 1. Extract values from incoming JSON packet
        String content = (String) payload.get("content");
        String localId = (String) payload.get("localId");
        Long currentPageId = Long.valueOf(String.valueOf(payload.get("currentPageId")));
        
        // Handle optional nullable anchor IDs
        Long previousWordId = payload.get("previousWordId") != null ? 
            Long.valueOf(String.valueOf(payload.get("previousWordId"))) : null;
        String previousLocalId = (String) payload.get("previousLocalId");

        // 2. Use your exact public constructor definition
        Word transientWord = new Word(content, localId);

        // 3. Hand it off to your transactional service
        Word savedDatabaseWord = wordService.createWord(transientWord, currentPageId, localId, previousWordId, previousLocalId);
        
        System.out.println("🎉 WebSocket saved word. ID assigned: " + savedDatabaseWord.getId());

        // 4. Broadcast the final database entity to the channel
        messagingTemplate.convertAndSend("/topic/page/" + currentPageId, (Object) savedDatabaseWord);
    }
}