package com.example.demo.gql.types;

import lombok.*;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    Long id;
    Long postId;
    String content;
}
