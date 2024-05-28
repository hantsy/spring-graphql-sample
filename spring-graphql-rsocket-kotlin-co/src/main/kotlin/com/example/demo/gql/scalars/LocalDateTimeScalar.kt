package com.example.demo.gql.scalars

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingSerializeException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LocalDateTimeScalar : Coercing<LocalDateTime, String> {
    @Throws(CoercingSerializeException::class)
    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String? {
        return when (dataFetcherResult) {
            is LocalDateTime -> dataFetcherResult.format(DateTimeFormatter.ISO_DATE_TIME)
            else -> throw CoercingSerializeException("Not a valid DateTime")
        }
    }

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): LocalDateTime {
        return LocalDateTime.parse(input.toString(), DateTimeFormatter.ISO_DATE_TIME)
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): LocalDateTime {
        when (input) {
            is StringValue -> return LocalDateTime.parse(input.value, DateTimeFormatter.ISO_DATE_TIME)
            else -> throw CoercingParseLiteralException("Value is not a valid ISO date time")
        }
    }

    override fun valueToLiteral(input: Any, graphQLContext: GraphQLContext, locale: Locale): Value<*> {
        return StringValue(input.toString())
    }

}