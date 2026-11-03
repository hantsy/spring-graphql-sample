package com.example.demo.gql.scalars;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


public class LocalDateTimeScalar implements Coercing<LocalDateTime, String> {
    @Override
    public String serialize(Object dataFetcherResult, GraphQLContext graphQLContext, Locale locale) throws CoercingSerializeException {
        if (dataFetcherResult instanceof LocalDateTime) {
            return ((LocalDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_DATE_TIME);
        } else {
            throw new CoercingSerializeException("Not a valid DateTime");
        }
    }

    @Override
    public LocalDateTime parseValue(Object input, GraphQLContext graphQLContext, Locale locale) throws CoercingParseValueException {
        return LocalDateTime.parse(input.toString(), DateTimeFormatter.ISO_DATE_TIME);
    }

    @Override
    public LocalDateTime parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext graphQLContext, Locale locale) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
            return LocalDateTime.parse(((StringValue) input).getValue(), DateTimeFormatter.ISO_DATE_TIME);
        }
        throw new CoercingParseLiteralException("Value is not a valid ISO date time");
    }

    @Override
    public Value<?> valueToLiteral(Object input, GraphQLContext graphQLContext, Locale locale) {
        return StringValue.newStringValue(input.toString()).build();
    }
}
