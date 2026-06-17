package com.example.restservice.dto;

public class AuthorDto {
    private String username;
    private Long likes = 0L;
    private Long dislikes = 0L;
    private Long creditsSpent = 0L;

    public AuthorDto(String username) {
        this.username = username;
    }

    public Long getLikes() {
        return likes;
    }

    public void setLikes(Long likes) {
        this.likes = likes;
    }

    public Long getDislikes() {
        return dislikes;
    }

    public void setDislikes(Long dislikes) {
        this.dislikes = dislikes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getCreditsSpent() {
        return creditsSpent;
    }

    public void setCreditsSpent(Long creditsSpent) {
        this.creditsSpent = creditsSpent;
    }
}
