package com.example.restservice.dto;

import com.example.restservice.enums.ReactionType;

public class ReactionUpdateEvent {
    private Long wordId;
    private ReactionType reactionType;
    private long totalCount;
    private String action; // "ADDED" or "REMOVED" (Helpful for frontend animations)

    public ReactionUpdateEvent(Long wordId, ReactionType reactionType, long totalCount, String action) {
        this.wordId = wordId;
        this.reactionType = reactionType;
        this.totalCount = totalCount;
        this.action = action;
    }

    // Getters and Setters
    public Long getWordId() { return wordId; }
    public ReactionType getReactionType() { return reactionType; }
    public long getTotalCount() { return totalCount; }
    public String getAction() { return action; }
}