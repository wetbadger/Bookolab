package com.example.restservice.dto;

public class WordNodeDto {
    private Long id;
    private String content;
    private WordNodeDto nextWord; // <-- Keeps the nested JSON structure

    public WordNodeDto(Long id, String content) {
        this.id = id;
        this.content = content;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getContent() { return content; }
    public WordNodeDto getNextWord() { return nextWord; }
    public void setNextWord(WordNodeDto nextWord) { this.nextWord = nextWord; }
}
