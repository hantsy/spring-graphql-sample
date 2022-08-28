package com.example.demo.gql

import com.example.demo.Scalars
import com.expediagroup.graphql.generator.directives.DirectiveMetaInformation
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLType
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KType

@Component
class CustomSchemaGeneratorHooks(override val wiringFactory: KotlinDirectiveWiringFactory) : SchemaGeneratorHooks {
    override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier) {
        UUID::class -> Scalars.uuidType()
        LocalDateTime::class -> Scalars.localDateTimeType()
        else -> null
    }

}