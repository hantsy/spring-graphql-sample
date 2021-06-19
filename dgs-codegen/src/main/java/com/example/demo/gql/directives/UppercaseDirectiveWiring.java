package com.example.demo.gql.directives;

import com.netflix.graphql.dgs.DgsComponent;
import graphql.schema.*;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;

public class UppercaseDirectiveWiring implements SchemaDirectiveWiring {
    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {

        var field = env.getElement();
        var parentType = env.getFieldsContainer();

        var originalDataFetcher = env.getCodeRegistry().getDataFetcher(parentType, field);
        var dataFetcher = DataFetcherFactories.wrapDataFetcher(originalDataFetcher,
                (dataFetchingEnvironment, value) -> {
                    if (value instanceof String s) {
                        return s.toUpperCase();
                    }
                    return value;
                }
        );

        env.getCodeRegistry().dataFetcher(parentType, field, dataFetcher);
        return field;
    }
}