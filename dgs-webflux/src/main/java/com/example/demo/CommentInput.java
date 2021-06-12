package com.example.demo;

import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CommentInput {
    String content;
    String postId;
}
