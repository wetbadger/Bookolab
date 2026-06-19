package com.bookolab.restservice.dto;

public record BoundedPageResponse(
    Long id,
    WordNodeDto firstWord,
    FlatLinkedWordDto lastWord,
    Long lastWordIdOfPreviousPage,
    Long totalPages
) {}
