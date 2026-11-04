package com.example.demo.gql.types;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class CommentInput {
    @NotEmpty
    @Length(min = 5, max = 250)
    String content;

    @NotEmpty
    String postId;
}
