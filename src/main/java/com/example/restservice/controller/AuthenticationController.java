package com.example.restservice.controller;

import com.example.restservice.dto.RegisterOrLoginAuthorDto;
import com.example.restservice.model.Author;
import com.example.restservice.response.LoginResponse;
import com.example.restservice.service.AuthenticationService;
import com.example.restservice.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/auth")
@RestController
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Author> register(@RequestBody RegisterOrLoginAuthorDto registerAuthorDto) {
        Author registeredAuthor = authenticationService.signup(registerAuthorDto);
        return ResponseEntity.ok(registeredAuthor);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody RegisterOrLoginAuthorDto loginAuthorDto) {
        Author authenticatedAuthor = authenticationService.authenticate(loginAuthorDto);
        String jwtToken = jwtService.generateToken(authenticatedAuthor);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }
}
