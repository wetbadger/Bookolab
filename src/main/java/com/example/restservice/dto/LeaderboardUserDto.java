package com.example.restservice.dto;

import java.io.Serializable;

public record LeaderboardUserDto(Long id, String username, Long score) implements Serializable {
    private static final long serialVersionUID = 1L;
}