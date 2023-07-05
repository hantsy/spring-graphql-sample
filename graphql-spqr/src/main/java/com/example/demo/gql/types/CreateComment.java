package com.example.demo.gql.types;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotEmpty;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class CreateComment {

    @NotEmpty
    String postId;

    @NotEmpty
    @Length(min = 10, max = 50)
    String content;
}
