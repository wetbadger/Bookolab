package com.bookolab.restservice.dto;

public record PageResponseDto(
    Long id,
    FlatLinkedWordDto firstWord,
    FlatLinkedWordDto lastWord
) {}
