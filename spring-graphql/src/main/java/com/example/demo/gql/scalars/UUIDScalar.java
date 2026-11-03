package com.example.demo.gql.scalars;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.util.Locale;
import java.util.UUID;

public class UUIDScalar implements Coercing<UUID, String> {
    @Override
    public String serialize(Object dataFetcherResult, GraphQLContext graphQLContext, Locale locale) throws CoercingSerializeException {
        if (dataFetcherResult instanceof UUID) {
            return ((UUID) dataFetcherResult).toString();
        } else {
            throw new CoercingSerializeException("Not a valid UUID");
        }
    }

    @Override
    public UUID parseValue(Object input, GraphQLContext graphQLContext, Locale locale) throws CoercingParseValueException {
        return UUID.fromString(input.toString());
    }

    @Override
    public UUID parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext graphQLContext, Locale locale) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
            return UUID.fromString(((StringValue) input).getValue());
        }
        throw new CoercingParseLiteralException("Value is not a valid UUID string");
    }

    @Override
    public Value<?> valueToLiteral(Object input, GraphQLContext graphQLContext, Locale locale) {
        return StringValue.newStringValue(input.toString()).build();
    }
}
