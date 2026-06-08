package com.example.restservice.dto;

public class FlatLinkedWordDto {
    private Long id;
    private String content;
    private Long nextWordId; // <-- Just the ID reference!

    public FlatLinkedWordDto(Long id, String content, Long nextWordId) {
        this.id = id;
        this.content = content;
        this.nextWordId = nextWordId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getContent() { return content; }
    public Long getNextWordId() { return nextWordId; }
    public void setNextWordId(Long nextWordId) { this.nextWordId = nextWordId; }
}
