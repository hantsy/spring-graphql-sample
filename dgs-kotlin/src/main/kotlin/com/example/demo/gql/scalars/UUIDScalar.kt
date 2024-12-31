package com.example.demo.gql.scalars

import com.netflix.graphql.dgs.DgsScalar
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingSerializeException
import java.util.*

@DgsScalar(name = "UUID")
class UUIDScalar : Coercing<UUID, String> {

    override fun valueToLiteral(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): Value<*> = StringValue.of(input.toString())

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): UUID? {
        if (input is StringValue) {
            return UUID.fromString(input.value);
        }

        throw CoercingParseLiteralException("Value is not a valid UUID string");
    }

    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): UUID? = UUID.fromString(input.toString())

    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): String? {
        if (dataFetcherResult is UUID) {
            return dataFetcherResult.toString();
        }

        throw CoercingSerializeException("Not a valid UUID");
    }
}