package com.bookolab.restservice.dto;

public class DeletionResult {
    private Long previousWordId;
    private WordNodeDto nextWord;
    boolean valid = true;

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

    public boolean getValid() {
        return this.valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
