package com.example.demo.gql.types;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class CommentInput {

    @NotEmpty
    String postId;

    @NotEmpty
    @Length(min = 10, max = 50)
    String content;
}
