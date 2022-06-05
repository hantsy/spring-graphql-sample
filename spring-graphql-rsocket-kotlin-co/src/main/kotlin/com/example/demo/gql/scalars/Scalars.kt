package com.example.demo

import com.example.demo.gql.scalars.LocalDateTimeScalar
import com.example.demo.gql.scalars.UUIDScalar
import graphql.schema.*


object Scalars {
    fun uuidType(): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .name("UUID")
            .description("UUID type")
            .coercing(UUIDScalar())
            .build()
    }

    fun localDateTimeType(): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .name("LocalDateTime")
            .description("LocalDateTime type")
            .coercing(LocalDateTimeScalar())
            .build()
    }
}

