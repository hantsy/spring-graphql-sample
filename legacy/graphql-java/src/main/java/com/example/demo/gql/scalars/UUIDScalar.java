package com.example.demo.gql.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.util.UUID;

public class UUIDScalar implements Coercing<UUID, String> {
    @Override
    public String serialize(Object o) throws CoercingSerializeException {
        if (o instanceof UUID) {
            return ((UUID) o).toString();
        } else {
            throw new CoercingSerializeException("Not a valid UUID");
        }
    }

    @Override
    public UUID parseValue(Object o) throws CoercingParseValueException {
        return UUID.fromString(o.toString());
    }

    @Override
    public UUID parseLiteral(Object input) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
            return UUID.fromString(((StringValue) input).getValue());
        }

        throw new CoercingParseLiteralException("Value is not a valid UUID string");
    }
}
