package com.example.demo.gql.directives;

import com.netflix.graphql.dgs.DgsDirective;
import graphql.schema.DataFetcherFactories;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import lombok.extern.slf4j.Slf4j;

@DgsDirective(name = "uppercase")
@Slf4j
public class UppercaseDirectiveWiring implements SchemaDirectiveWiring {
    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {

        var field = env.getElement();
        var parentType = env.getFieldsContainer();
        var fieldCoordinates = FieldCoordinates.coordinates(parentType, field.getName());
        var originalDataFetcher = env.getCodeRegistry().getDataFetcher(fieldCoordinates, field);
        var dataFetcher = DataFetcherFactories.wrapDataFetcher(
                originalDataFetcher,
                (dataFetchingEnvironment, value) -> {
                    if (value instanceof String s) {
                        log.info("@uppercase: {}", s);
                        return s.toUpperCase();
                    }
                    return value;
                }
        );

        env.getCodeRegistry().dataFetcher(fieldCoordinates, dataFetcher);
        return field;
    }
}