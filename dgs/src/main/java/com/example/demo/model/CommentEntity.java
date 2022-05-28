package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentEntity(
        Long id,
        String content,
        Long postId
) {
}
