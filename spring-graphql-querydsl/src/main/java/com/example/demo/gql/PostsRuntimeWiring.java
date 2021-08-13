package com.example.demo.gql;

import com.example.demo.gql.directives.UpperCaseDirectiveWiring;
import com.example.demo.gql.scalars.Scalars;
import com.example.demo.repository.PostRepository;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeRuntimeWiring;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.querydsl.QuerydslDataFetcher;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostsRuntimeWiring implements RuntimeWiringConfigurer {
    final PostRepository repository;

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        builder
                // auto registered in starter.
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("posts", QuerydslDataFetcher.builder(repository).many())
                        .dataFetcher("post", QuerydslDataFetcher.builder(repository).single())
                )
                .scalar(Scalars.uuidType())
                .scalar(Scalars.localDateTimeType())
                .directive("uppercase", new UpperCaseDirectiveWiring())
                .build();
    }

}
