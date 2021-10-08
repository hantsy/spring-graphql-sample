package com.example.demo.gql.types;

import io.leangen.graphql.annotations.GraphQLId;
import lombok.*;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @GraphQLId
    String id;
    String postId;
    String content;
}
