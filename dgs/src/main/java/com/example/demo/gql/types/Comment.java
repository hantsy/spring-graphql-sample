package com.example.demo.gql.types;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class Comment {
    Long id;
    Long postId;
    String content;
}
