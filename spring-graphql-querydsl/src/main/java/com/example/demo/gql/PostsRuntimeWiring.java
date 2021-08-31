package com.example.demo.gql;

import com.example.demo.repository.PostRepository;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeRuntimeWiring;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.querydsl.QuerydslDataFetcher;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostsRuntimeWiring implements RuntimeWiringConfigurer {
    final PostRepository repository;

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        builder
                // should be registered automatically in spring boot starter???
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("posts", QuerydslDataFetcher.builder(repository).many())
                        .dataFetcher("post", QuerydslDataFetcher.builder(repository).single())
                )
                .build();
    }

}
