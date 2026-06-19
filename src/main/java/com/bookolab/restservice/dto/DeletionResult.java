package com.bookolab.restservice.dto;

public class DeletionResult {
    private Long previousWordId;
    private WordNodeDto nextWord;

    public DeletionResult(Long previousWordId, WordNodeDto nextWord) {
        this.previousWordId = previousWordId;
        this.nextWord = nextWord;
    }

    public Long getPreviousWordId() {
        return previousWordId;
    }

    public void setPreviousWordId(Long previousWordId) {
        this.previousWordId = previousWordId;
    }

    public WordNodeDto getNextWord() {
        return nextWord;
    }

    public void setNextWord(WordNodeDto nextWord) {
        this.nextWord = nextWord;
    }
}
