package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthorEntity(
        UUID id,
        String name,
        String email,
        LocalDateTime createdAt
) {
}

