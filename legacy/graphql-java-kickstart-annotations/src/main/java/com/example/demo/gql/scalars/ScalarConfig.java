package com.example.demo.gql.scalars;

import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScalarConfig {

    @Bean
    GraphQLScalarType uuidType() {
        return GraphQLScalarType.newScalar()
                .name("UUID")
                .description("UUID type")
                .coercing(new UUIDScalar())
                .build();
    }

    @Bean
    GraphQLScalarType localDateTimeType() {
        return GraphQLScalarType.newScalar()
                .name("LocalDateTime")
                .description("LocalDateTime type")
                .coercing(new LocalDateTimeScalar())
                .build();
    }
}
