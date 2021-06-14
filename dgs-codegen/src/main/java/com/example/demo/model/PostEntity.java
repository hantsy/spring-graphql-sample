package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostEntity(
        UUID id,
        String title,
        String content,
        String status,
        LocalDateTime createdAt,
        UUID authorId
) {

}
