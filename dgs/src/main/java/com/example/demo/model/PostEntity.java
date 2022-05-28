package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostEntity(
        Long id,
        String title,
        String content,
        String status,
        Long authorId
) {

}
