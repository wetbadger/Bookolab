package com.example.restservice.dto;

public record PageResponseDto(
    Long id,
    FlatLinkedWordDto firstWord,
    FlatLinkedWordDto lastWord
) {}
