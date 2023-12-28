package com.example.demo.gql.types;

import com.example.demo.gql.directives.UpperCaseDirective;
import com.example.demo.gql.resolvers.AuthorDataFetcher;
import com.example.demo.gql.resolvers.CommentsDataFetcher;
import graphql.annotations.annotationTypes.GraphQLDataFetcher;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLID;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @GraphQLID
    @GraphQLField
    @GraphQLNonNull
    private String id;

    @GraphQLField
    @GraphQLNonNull
    @UpperCaseDirective
    private String title;

    @GraphQLField
    private String content;

    @Builder.Default
    @GraphQLField
    @GraphQLDataFetcher(CommentsDataFetcher.class)
    private List<Comment> comments= new ArrayList<>();

    @GraphQLField
    private PostStatus status;

    @GraphQLField
    private LocalDateTime createdAt;

    @GraphQLField
    private String authorId;

    @GraphQLField
    @GraphQLDataFetcher(AuthorDataFetcher.class)
    private Author author;
}
