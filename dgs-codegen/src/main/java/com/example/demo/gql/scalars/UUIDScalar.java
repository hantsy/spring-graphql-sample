package com.example.demo.gql.scalars;

import com.netflix.graphql.dgs.DgsScalar;
import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

@DgsScalar(name = "UUID")
public class UUIDScalar implements Coercing<UUID, String> {

    @Override
    public @NotNull Value<?> valueToLiteral(@NotNull Object input, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) {
        return StringValue.of(input.toString());
    }

    @Nullable
    @Override
    public UUID parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
        if (input instanceof StringValue value) {
            return UUID.fromString(value.getValue());
        }

        throw new CoercingParseLiteralException("Value is not a valid UUID string");
    }

    @Nullable
    @Override
    public UUID parseValue(@NotNull Object input, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseValueException {
        return UUID.fromString(input.toString());
    }

    @Nullable
    @Override
    public String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {
        if (dataFetcherResult instanceof UUID uuid) {
            return uuid.toString();
        }

        throw new CoercingSerializeException("Not a valid UUID");
    }
}
