package com.example.restservice.service;

import com.example.restservice.model.Author;
import com.example.restservice.repository.AuthorRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<Author> allAuthors() {
        List<Author> authors = new ArrayList<>();
        authorRepository.findAll().forEach(authors::add);
        return authors;
    }

    public Author getAuthorByName(String name) {
        return authorRepository.findAuthorByUsername(name)
                .orElseThrow(() -> new UsernameNotFoundException("Author not found"));
    }
}
