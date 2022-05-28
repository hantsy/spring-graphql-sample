package com.example.demo.gql.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    private String id;
    private String title;
    private String content;
    @Builder.Default
    private List<Comment> comments= new ArrayList<>();
    private PostStatus status;
    private LocalDateTime createdAt;
    private String authorId;
    private Author author;
}
