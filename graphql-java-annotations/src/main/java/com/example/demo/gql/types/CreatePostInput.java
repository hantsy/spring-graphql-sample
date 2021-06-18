package com.example.demo.gql.types;

import graphql.annotations.annotationTypes.GraphQLConstructor;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @GraphQLConstructor)
public class CreatePostInput {
    @GraphQLField
    @GraphQLNonNull
    private String title;
    @GraphQLField
    @GraphQLNonNull
    private String content;
}
