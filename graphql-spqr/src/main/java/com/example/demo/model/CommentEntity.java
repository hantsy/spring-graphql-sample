package com.example.demo.model;

import java.util.UUID;

public record CommentEntity(
        UUID id,
        String content,
        UUID postId
) {
}
