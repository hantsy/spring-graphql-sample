package com.example.demo.gql.types;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class CreatePostInput {
    @NotEmpty
    @Length(min = 5, max = 100)
    String title;

    @NotEmpty
    String content;
}
