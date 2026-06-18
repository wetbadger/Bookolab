package com.example.restservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // This automatically turns it into a 409 Conflict status
public class AnonymousException extends RuntimeException {
    public AnonymousException(String message) {
        super(message);
    }
}