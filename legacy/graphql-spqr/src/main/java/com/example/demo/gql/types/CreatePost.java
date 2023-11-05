package com.example.demo.gql.types;

import io.leangen.graphql.annotations.GraphQLInputField;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotEmpty;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
// The generator will add `Input` postfix automatically.
public class CreatePost {
    @NotEmpty
    @Length(min = 5, max = 100)
    @GraphQLInputField
    String title;

    @NotEmpty
    @GraphQLInputField
    String content;
}
