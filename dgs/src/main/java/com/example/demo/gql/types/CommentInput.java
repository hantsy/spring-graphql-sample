package com.example.demo.gql.types;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CommentInput {

    @NotEmpty
    @Length(min = 10, max = 50)
    String content;

    @NotEmpty
    String postId;
}
