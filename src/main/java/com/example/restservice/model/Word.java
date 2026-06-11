package com.example.restservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.GeneratedValue;

@Entity
public class Word {
    @Id
    @GeneratedValue
    private Long id;

    private String previousLocalId;

    @Column(nullable = false)
    private String localId;

    @Column(nullable = false, length = 30)
    private String content;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Word nextWord;

    protected Word() {}

    public Word(String content, String localId) {
        this.content = content;
        this.localId = localId;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String name) {
        this.content = name;
    }

    public Long getId() {
        return this.id;
    }

    public void setNextWord(Word word) {
        this.nextWord = word;
    }

    public Word getNextWord() {
        return this.nextWord;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getLocalId() {
        return this.localId;
    }

    @Override
    public String toString() {
        return this.content;
    }
}