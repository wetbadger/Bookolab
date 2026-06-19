package com.bookolab.restservice.exception;

import com.bookolab.restservice.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        // Automatically reads the message you put inside: throw new UsernameAlreadyExistsException(...)
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409 Conflict
                .body(new ErrorResponseDto(ex.getMessage()));
    }
}