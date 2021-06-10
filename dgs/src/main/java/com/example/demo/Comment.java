package com.example.demo;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class Comment {
    String id;
    String postId;
    String content;
}
