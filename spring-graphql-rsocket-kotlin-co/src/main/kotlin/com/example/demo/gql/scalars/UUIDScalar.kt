package com.example.demo.gql.scalars

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingSerializeException
import java.util.*

class UUIDScalar : Coercing<UUID, String> {

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): UUID? {
        if (input is StringValue) {
            return UUID.fromString(input.value)
        }
        throw CoercingParseLiteralException("Value is not a valid UUID string")
    }

    override fun valueToLiteral(input: Any, graphQLContext: GraphQLContext, locale: Locale): Value<*> {
        return StringValue(input.toString())
    }

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): UUID? {
        return UUID.fromString(input.toString())
    }

    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String? {
        return when (dataFetcherResult) {
            is UUID -> dataFetcherResult.toString()
            else -> throw CoercingSerializeException("Not a valid UUID")
        }
    }
}