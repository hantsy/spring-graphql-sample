package com.example.demo.gql.types;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Post {
    Long id;
    String title;
    String content;

    @Builder.Default
    List<Comment> comments = new ArrayList<>();
    Long authorId;
    Author author;
}
