package com.example.demo.gql;

import com.example.demo.gql.directives.UpperCaseDirectiveWiring;
import com.example.demo.gql.scalars.Scalars;
import graphql.GraphQL;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import graphql.schema.PropertyDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.SneakyThrows;
import org.dataloader.DataLoaderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

@Configuration
public class GraphQLConfig {

    @Bean
    DataLoaderRegistry dataLoaderRegistry(DataLoaders dataLoaders) {
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register("authorsLoader", dataLoaders.authorsLoader());
        registry.register("commentsLoader", dataLoaders.commentsLoader());

        return registry;
    }

    @SneakyThrows
    @Bean
    public TypeDefinitionRegistry typeDefinitionRegistry() {
        var schema = new ClassPathResource("/schema/schema.graphql");
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(schema.getFile());
        return typeRegistry;
    }

    @Bean
    public GraphQLSchema graphQLSchema(TypeDefinitionRegistry typeRegistry, RuntimeWiring runtimeWiring) {
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    @Bean
    public GraphQLCodeRegistry codeRegistry(DataFetchers dataFetchers) {
        return GraphQLCodeRegistry.newCodeRegistry()
                .dataFetchers("Query", Map.of(
                        "postById", dataFetchers.getPostById(),
                        "allPosts", dataFetchers.getAllPosts()
                ))
                .dataFetchers("Mutation", Map.of(
                        "createPost", dataFetchers.createPost()
                ))
                .dataFetchers("Post", Map.of(
                        "author", dataFetchers.authorOfPost(),
                        "comments", dataFetchers.commentsOfPost()
                ))
                //.typeResolver()
                //.fieldVisibility()
                .defaultDataFetcher(environment -> PropertyDataFetcher.fetching(environment.getFieldDefinition().getName()))
                .build();
    }

    @Bean
    RuntimeWiring buildWiring(GraphQLCodeRegistry codeRegistry) {
        return RuntimeWiring.newRuntimeWiring()
                .codeRegistry(codeRegistry)
                .scalar(Scalars.localDateTimeType())
                .scalar(Scalars.uuidType())
                .directive("uppercase", new UpperCaseDirectiveWiring())
                .build();
    }

    @Bean
    public GraphQL graphQL(GraphQLSchema graphQLSchema) {
        return GraphQL.newGraphQL(graphQLSchema)
                .defaultDataFetcherExceptionHandler(new CustomDataFetchingExceptionHandler())
                //.queryExecutionStrategy()
                //.mutationExecutionStrategy()
                //.subscriptionExecutionStrategy()
                //.instrumentation()
                .build();
    }

}