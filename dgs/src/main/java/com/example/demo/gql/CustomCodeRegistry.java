package com.example.demo.gql;

import com.netflix.graphql.dgs.DgsCodeRegistry;
import com.netflix.graphql.dgs.DgsComponent;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.time.LocalDateTime;

@DgsComponent
public class CustomCodeRegistry {

    @DgsCodeRegistry
    public GraphQLCodeRegistry.Builder extraCodeRegistry(GraphQLCodeRegistry.Builder codeRegistryBuilder, TypeDefinitionRegistry registry) {

        DataFetcher<String> df = (dfe) -> "Current timestamp is " + LocalDateTime.now();
        FieldCoordinates coordinates = FieldCoordinates.coordinates("Query", "currentTimestamp");

        return codeRegistryBuilder.dataFetcher(coordinates, df);
    }
}
