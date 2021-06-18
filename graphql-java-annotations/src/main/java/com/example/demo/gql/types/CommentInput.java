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
public class CommentInput {
    @GraphQLField
    @GraphQLNonNull
    private String postId;

    @GraphQLField
    @GraphQLNonNull
    private String content;
}
