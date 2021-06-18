package com.example.demo.gql.directives;

import graphql.annotations.directives.AnnotationsDirectiveWiring;
import graphql.annotations.directives.AnnotationsWiringEnvironment;
import graphql.annotations.processor.util.CodeRegistryUtil;
import graphql.schema.*;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;

public class UpperCaseDirectiveWiring implements AnnotationsDirectiveWiring {

    @Override
    public GraphQLFieldDefinition onField(final AnnotationsWiringEnvironment env) {
        final GraphQLFieldDefinition field = (GraphQLFieldDefinition) env.getElement();
        CodeRegistryUtil.wrapDataFetcher(
                field,
                env,
                (((dataFetchingEnvironment, value) -> {
                    if (value instanceof String) {
                        return ((String) value).toUpperCase();
                    }
                    return value;
                })));
        return field;
    }
}