package com.example.demo.gql.directives

import com.expediagroup.graphql.generator.directives.KotlinFieldDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import graphql.schema.DataFetcherFactories
import graphql.schema.GraphQLFieldDefinition
import java.util.*

class UpperCaseDirectiveWiring : KotlinSchemaDirectiveWiring {
    override fun onField(env: KotlinFieldDirectiveEnvironment): GraphQLFieldDefinition {

        val field = env.element
        val dataFetcher = DataFetcherFactories
            .wrapDataFetcher(
                env.getDataFetcher()
            ) { _, value -> if (value is String && value.isNotEmpty()) value.uppercase(Locale.getDefault()) else value }

        env.setDataFetcher(dataFetcher);
        return field
    }
}
