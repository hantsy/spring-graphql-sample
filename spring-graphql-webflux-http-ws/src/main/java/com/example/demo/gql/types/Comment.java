package com.example.demo.gql.types;

import lombok.*;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    String id;
    String postId;
    String content;
}
