package com.example.demo.gql.types;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CreatePostInput {
    @NotEmpty
    @Length(min = 5, max = 100)
    String title;

    @NotEmpty
    @Length(min = 5, max = 1000)
    String content;
}
