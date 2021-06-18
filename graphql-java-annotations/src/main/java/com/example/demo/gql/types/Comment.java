package com.example.demo.gql.types;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLID;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @GraphQLID
    @GraphQLField
    @GraphQLNonNull
    private String id;

    @GraphQLField
    @GraphQLNonNull
    private String content;

    @GraphQLField
    private LocalDateTime createdAt;

    @GraphQLField
    private String postId;
}
