package com.example.demo.model;

public record PostEntity(
        Long id,
        String title,
        String content,
        String status,
        Long authorId
) {

}
