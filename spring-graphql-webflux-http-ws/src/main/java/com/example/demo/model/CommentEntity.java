package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentEntity(
        UUID id,
        String content,
        LocalDateTime createdAt,
        UUID postId
) {
}
