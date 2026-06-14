package com.example.restservice.controller;

import com.example.restservice.model.Author;
import com.example.restservice.service.AuthorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/authors")
@RestController
public class AuthorController {
    private final AuthorService authorService;
    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("/me")
    public ResponseEntity<Author> authenticatedAuthor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Author currentAuthor = (Author) authentication.getPrincipal();
        return ResponseEntity.ok(currentAuthor);
    }

    @GetMapping("/")
    public ResponseEntity<List<Author>> allAuthors() {
        List<Author> authors = authorService.allAuthors();
        return ResponseEntity.ok(authors);
    }
}
