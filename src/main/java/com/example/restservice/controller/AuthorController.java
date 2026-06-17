package com.example.restservice.controller;

import com.example.restservice.dto.AuthorDto;
import com.example.restservice.model.Author;
import com.example.restservice.repository.ReactionRepository;
import com.example.restservice.service.AuthorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/authors")
@RestController
public class AuthorController {
    private final ReactionRepository reactionRepository;
    private final AuthorService authorService;
    public AuthorController(ReactionRepository reactionRepository, AuthorService authorService)
    {
        this.reactionRepository = reactionRepository;
        this.authorService = authorService;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthorDto> authenticatedAuthor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Author currentAuthor = (Author) authentication.getPrincipal();
        AuthorDto currentAuthorDto = new AuthorDto(currentAuthor.getUsername());
        currentAuthorDto.setScore(reactionRepository.countLikesMinusDislikes(currentAuthor.getId()));
        currentAuthorDto.setCreditsSpent(currentAuthor.getCreditsSpent());
        return ResponseEntity.ok(currentAuthorDto);
    }

    @GetMapping("/")
    public ResponseEntity<List<Author>> allAuthors() {
        List<Author> authors = authorService.allAuthors();
        return ResponseEntity.ok(authors);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Author> getAuthor(@PathVariable String name) {
        Author author = authorService.getAuthorByName(name);
        return ResponseEntity.ok(author);
    }
}
