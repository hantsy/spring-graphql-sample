package com.example.demo

import com.netflix.graphql.dgs.DgsScalar
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@DgsScalar(name = "LocalDateTime")
class LocalDateTimeScalar : Coercing<LocalDateTime, String> {
    @Throws(CoercingSerializeException::class)
    override fun serialize(dataFetcherResult: Any): String? {
        return when (dataFetcherResult) {
            is LocalDateTime -> dataFetcherResult.format(DateTimeFormatter.ISO_DATE_TIME)
            else -> throw CoercingSerializeException("Not a valid DateTime")
        }
    }

    @Throws(CoercingParseValueException::class)
    override fun parseValue(input: Any): LocalDateTime {
        return LocalDateTime.parse(input.toString(), DateTimeFormatter.ISO_DATE_TIME)
    }

    @Throws(CoercingParseLiteralException::class)
    override fun parseLiteral(input: Any): LocalDateTime {
        when (input) {
            is StringValue -> return LocalDateTime.parse(input.value, DateTimeFormatter.ISO_DATE_TIME)
            else -> throw CoercingParseLiteralException("Value is not a valid ISO date time")
        }
    }

}