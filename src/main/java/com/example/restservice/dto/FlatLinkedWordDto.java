package com.example.restservice.dto;

public class FlatLinkedWordDto {
    private Long id;
    private String content;
    private Long nextWordId;
    private Long previousWordId;

    public FlatLinkedWordDto(Long id, String content, Long nextWordId, Long previousWordId) {
        this.id = id;
        this.content = content;
        this.nextWordId = nextWordId;
        this.previousWordId = previousWordId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getContent() { return content; }
    public Long getNextWordId() { return nextWordId; }
    public void setNextWordId(Long nextWordId) { this.nextWordId = nextWordId; }
    public Long getPreviousWordId() { return previousWordId; }
    public void setPreviousWordId(Long previousWordId) { this.previousWordId = previousWordId; }
}
