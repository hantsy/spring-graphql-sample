package com.example.demo.gql.scalars

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingSerializeException
import java.util.*

class UUIDScalar : Coercing<UUID, String> {
    override fun serialize(o: Any): String {
        return when (o) {
            is UUID -> o.toString()
            else -> throw CoercingSerializeException("Not a valid UUID")
        }
    }

    override fun parseValue(o: Any): UUID {
        return UUID.fromString(o.toString())
    }

    override fun parseLiteral(input: Any): UUID {
        if (input is StringValue) {
            return UUID.fromString(input.value)
        }
        throw CoercingParseLiteralException("Value is not a valid UUID string")
    }
}