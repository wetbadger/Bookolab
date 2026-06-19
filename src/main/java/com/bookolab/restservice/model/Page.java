package com.bookolab.restservice.model;

import jakarta.persistence.*;

@Entity
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "first_word_id", unique = true)
    Word firstWord;

    @OneToOne
    @JoinColumn(name = "last_word_id", unique = true)
    Word lastWord;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Word getFirstWord() {
        return this.firstWord;
    }

    public void setFirstWord(Word word) {
        this.firstWord = word;
    }

    public Word getLastWord() {
        return this.lastWord;
    }

    public void setLastWord(Word word) {
        this.lastWord = word;
    }
}
