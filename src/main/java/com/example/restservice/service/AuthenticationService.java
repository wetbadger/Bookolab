package com.example.restservice.service;

import com.example.restservice.dto.RegisterOrLoginAuthorDto;
import com.example.restservice.model.Author;
import com.example.restservice.repository.AuthorRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
        Author author = new Author(input.getUsername(), passwordEncoder.encode(input.getPassword()));
        author.setEnabled(true); // TODO: start disabled and do something to verify that they are legit, like check the num of accounts on their ip
        // sendVerificationEmail would be here

        return authorRepository.save(author);
    }

    public Author authenticate(RegisterOrLoginAuthorDto input) {
        Author author = authorRepository.findAuthorByUsername(input.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

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
}
