package com.example.demo.gql

import com.example.demo.gql.scalars.Scalars
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.hooks.FlowSubscriptionSchemaGeneratorHooks
import graphql.schema.GraphQLType
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KType

class CustomSchemaGeneratorHooks(override val wiringFactory: KotlinDirectiveWiringFactory) :
    FlowSubscriptionSchemaGeneratorHooks() {
    override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier) {
        UUID::class -> Scalars.uuidType // use val instead of fun, else a duplication exception thrown.
        LocalDateTime::class -> Scalars.localDateTimeType
        else -> null
    }

}