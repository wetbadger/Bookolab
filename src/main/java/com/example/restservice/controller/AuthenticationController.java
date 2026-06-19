package com.example.restservice.controller;

import com.example.restservice.dto.ErrorResponseDto;
import com.example.restservice.dto.RegisterOrLoginAuthorDto;
import com.example.restservice.model.Author;
import com.example.restservice.response.LoginResponse;
import com.example.restservice.service.AuthenticationService;
import com.example.restservice.service.JwtService;
import com.example.restservice.service.SlurDetector;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth")
@RestController
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    private final SlurDetector slurDetector;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService, SlurDetector slurDetector) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
        this.slurDetector = slurDetector;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody RegisterOrLoginAuthorDto registerAuthorDto) {
        // 1. Check for password security (400 Bad Request is best practice here)
        if (!isPasswordSecure(registerAuthorDto.getPassword())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("Password must be at least 8 characters long."));
        }

        if (slurDetector.isSlur(registerAuthorDto.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("Username must not contain ethnic slurs."));
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

    @DeleteMapping("/delete-current-account")
    public ResponseEntity<?> deleteCurrentAccount() {
        // 1. Grab the authentication object from the context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Extract the principal (usually your UserDetails object or username)
        assert authentication != null;
        Author author = (Author) authentication.getPrincipal();

        authenticationService.deleteAccount(author);

        // Your deletion logic here...
        return ResponseEntity.ok().build();
    }

    private boolean isPasswordSecure(String password) {
        return password.length() >= 8;
    }
}
