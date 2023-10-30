package com.example.demo.model;

public record CommentEntity(
        Long id,
        String content,
        Long postId
) {
}
