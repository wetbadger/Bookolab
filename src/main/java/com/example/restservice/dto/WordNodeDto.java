package com.example.restservice.dto;

import com.example.restservice.model.Word;

public class WordNodeDto {
    private Long id;
    private String content;
    private WordNodeDto nextWord; // <-- Keeps the nested JSON structure
    private long likeCount = 0;
    private long dislikeCount = 0;
    private String authorName;
    private boolean userLiked;
    private boolean userDisliked;

    public WordNodeDto(Long id, String content, String authorName) {
        this.id = id;
        this.content = content;
        this.authorName = authorName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getContent() { return content; }
    public WordNodeDto getNextWord() { return nextWord; }
    public void setNextWord(WordNodeDto nextWord) { this.nextWord = nextWord; }
    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
    public long getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(long dislikeCount) { this.dislikeCount = dislikeCount; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public boolean isUserLiked() {
        return userLiked;
    }

    public void setUserLiked(boolean userLiked) {
        this.userLiked = userLiked;
    }

    public boolean isUserDisliked() {
        return userDisliked;
    }

    public void setUserDisliked(boolean userDisliked) {
        this.userDisliked = userDisliked;
    }
}
