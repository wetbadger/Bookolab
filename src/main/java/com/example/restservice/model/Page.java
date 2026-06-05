package com.example.restservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class Page {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    Word firstWord;

    public Word getFirstWord() {
        return this.firstWord;
    }

    public void setFirstWord(Word word) {
        this.firstWord = word;
    }

}
