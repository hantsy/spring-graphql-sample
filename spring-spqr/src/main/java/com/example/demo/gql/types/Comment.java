package com.example.demo.gql.types;

import io.leangen.graphql.annotations.GraphQLId;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class Comment {
    @GraphQLId
    String id;
    String postId;
    String content;
}
