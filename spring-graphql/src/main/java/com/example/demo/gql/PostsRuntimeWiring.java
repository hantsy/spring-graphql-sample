package com.example.demo.gql;

import com.example.demo.gql.directives.UpperCaseDirectiveWiring;
import com.example.demo.gql.resolvers.PostsDataFetchers;
import com.example.demo.gql.scalars.Scalars;
import graphql.schema.idl.RuntimeWiring;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.boot.RuntimeWiringCustomizer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostsRuntimeWiring implements RuntimeWiringCustomizer {
    final PostsDataFetchers postsDataFetchers;

    @Override
    public void customize(RuntimeWiring.Builder builder) {
        builder
                .type("Query",
                        typeWiring -> typeWiring
                                .dataFetcher("allPosts", postsDataFetchers.allPosts())
                                .dataFetcher("postById", postsDataFetchers.postById())
                )
                .type("Mutation",
                        typeWiring -> typeWiring
                                .dataFetcher("createPost", postsDataFetchers.createPost())
                )
                .type("Post", typeWiring -> typeWiring
                        .dataFetcher("author", postsDataFetchers.authorOfPost())
                        .dataFetcher("comments", postsDataFetchers.commentsOfPost())

                )
                .scalar(Scalars.uuidType())
                .scalar(Scalars.localDateTimeType())
                .directive("uppercase", new UpperCaseDirectiveWiring())
                .build();
    }
}
