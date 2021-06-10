package com.example.demo;

import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CreatePostInput {
    String title;
    String content;
}
