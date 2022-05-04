package com.example.demo.gql.directives;

import graphql.schema.DataFetcherFactories;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;

public class UpperCaseDirectiveWiring implements SchemaDirectiveWiring {
    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {

        var field = env.getElement();
        //var parentType = env.getFieldsContainer();
        //var originalDataFetcher = env.getCodeRegistry().getDataFetcher(parentType, field);
        var dataFetcher = DataFetcherFactories.wrapDataFetcher(env.getFieldDataFetcher(),
                (dataFetchingEnvironment, value) -> {
                    if (value instanceof String s) {
                        return s.toUpperCase();
                    }
                    return value;
                }
        );

        //env.getCodeRegistry().dataFetcher(FieldCoordinates.coordinates(parentType, field), dataFetcher);
        env.setFieldDataFetcher(dataFetcher);
        return field;
    }
}