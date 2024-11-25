package com.example.demo.gql.scalars

import com.netflix.graphql.dgs.DgsScalar
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@DgsScalar(name = "LocalDateTime")
class LocalDateTimeScalar : Coercing<LocalDateTime, String> {
    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String? {
        return when (dataFetcherResult) {
            is LocalDateTime -> dataFetcherResult.format(DateTimeFormatter.ISO_DATE_TIME)
            else -> throw CoercingSerializeException("Not a valid DateTime")
        }
    }

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): LocalDateTime? {
        return LocalDateTime.parse(input.toString(), DateTimeFormatter.ISO_DATE_TIME)
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): LocalDateTime? {
        when (input) {
            is StringValue -> return LocalDateTime.parse(input.value, DateTimeFormatter.ISO_DATE_TIME)
            else -> throw CoercingParseLiteralException("Value is not a valid ISO date time")
        }
    }

    override fun valueToLiteral(input: Any, graphQLContext: GraphQLContext, locale: Locale): Value<*> {
        return when (input) {
            is String -> StringValue.newStringValue(input).build()
            else -> throw CoercingParseValueException("Value is not a string")
        }
    }
}
