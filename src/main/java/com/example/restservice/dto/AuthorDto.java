package com.example.restservice.dto;

public class AuthorDto {
    private String username;
    private Long score = 0L;
    private Long creditsSpent = 0L;

    public AuthorDto(String username) {
        this.username = username;
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

    public Long getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }
}
