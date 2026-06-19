package com.bookolab.restservice.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class Word {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String localId;

    @Column(nullable = false, length = 30)
    private String content;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Word nextWord;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Author author;

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

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }
}