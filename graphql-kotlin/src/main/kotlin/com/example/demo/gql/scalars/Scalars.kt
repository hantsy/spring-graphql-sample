package com.example.demo.gql.scalars

import graphql.schema.GraphQLScalarType


object Scalars {
    val uuidType: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("UUID")
        .description("UUID type")
        .coercing(UUIDScalar())
        .build()
    val localDateTimeType: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("LocalDateTime")
        .description("LocalDateTime type")
        .coercing(LocalDateTimeScalar())
        .build()

}

