package com.bookolab.restservice.dto;

public class FlatLinkedWordDto {
    private Long id;
    private String content;
    private Long nextWordId;
    private Long previousWordId;
    private String authorName;
    private long likeCount = 0;
    private long dislikeCount = 0;

    public FlatLinkedWordDto(Long id, String content, Long nextWordId, Long previousWordId, String authorName) {
        this.id = id;
        this.content = content;
        this.nextWordId = nextWordId;
        this.previousWordId = previousWordId;
        this.authorName = authorName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getContent() { return content; }
    public Long getNextWordId() { return nextWordId; }
    public void setNextWordId(Long nextWordId) { this.nextWordId = nextWordId; }
    public Long getPreviousWordId() { return previousWordId; }
    public void setPreviousWordId(Long previousWordId) { this.previousWordId = previousWordId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
    public void setDislikeCount(long dislikeCount) { this.dislikeCount = dislikeCount; }
    public long getLikeCount() { return likeCount; }
    public long getDislikeCount() { return dislikeCount; }
}
