package com.example.restservice.service;

import com.example.restservice.dto.RegisterOrLoginAuthorDto;
import com.example.restservice.exception.AnonymousException;
import com.example.restservice.exception.UsernameAlreadyExistsException;
import com.example.restservice.model.Author;
import com.example.restservice.repository.AuthorRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final AuthorRepository authorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            AuthorRepository authorRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager
    ) {
        this.authorRepository = authorRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public Author signup(RegisterOrLoginAuthorDto input) {
        if (input.getUsername().equals("Anonymous")) {
            throw new AnonymousException("Name cannot be Anonymous");
        }
        // 1. Check if the username is already taken
        if (authorRepository.existsByUsername(input.getUsername())) {
            throw new UsernameAlreadyExistsException("Username '" + input.getUsername() + "' is already taken.");
        }

        // 2. Proceed with creation if it's unique
        Author author = new Author(input.getUsername(), passwordEncoder.encode(input.getPassword()));
        author.setEnabled(true);

        return authorRepository.save(author);
    }

    public Author authenticate(RegisterOrLoginAuthorDto input) {
        Author author = authorRepository.findAuthorByUsername(input.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!author.isEnabled()) {
            throw new RuntimeException("Account not enabled");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getUsername(),
                        input.getPassword()
                )
        );

        return author;
    }

    public void deleteAccount(Author author) {
        authorRepository.deleteById(author.getId());
    }
}
