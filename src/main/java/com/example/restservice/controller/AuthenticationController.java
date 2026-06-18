package com.example.restservice.controller;

import com.example.restservice.dto.ErrorResponseDto;
import com.example.restservice.dto.RegisterOrLoginAuthorDto;
import com.example.restservice.model.Author;
import com.example.restservice.response.LoginResponse;
import com.example.restservice.service.AuthenticationService;
import com.example.restservice.service.JwtService;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> register(@RequestBody RegisterOrLoginAuthorDto registerAuthorDto) {
        // 1. Check for password security (400 Bad Request is best practice here)
        if (!isPasswordSecure(registerAuthorDto.getPassword())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("Password must be at least 8 characters long."));
        }

        // 2. Delegate to service. If it throws UsernameAlreadyExistsException,
        // the @RestControllerAdvice intercepts it instantly!
        Author author = authenticationService.signup(registerAuthorDto);

        return ResponseEntity.ok(author);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody RegisterOrLoginAuthorDto loginAuthorDto) {
        Author authenticatedAuthor = authenticationService.authenticate(loginAuthorDto);
        String jwtToken = jwtService.generateToken(authenticatedAuthor);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

    private boolean isPasswordSecure(String password) {
        return password.length() >= 8;
    }
}
