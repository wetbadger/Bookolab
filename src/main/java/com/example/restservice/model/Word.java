package com.example.restservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;

@Entity
public class Word {
    @Id
    @GeneratedValue
    private Long id;

    private String content;

    protected Word() {}

    public Word(String name) {
        this.content = name;
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

    @Override
    public String toString() {
        return this.content;
    }
}