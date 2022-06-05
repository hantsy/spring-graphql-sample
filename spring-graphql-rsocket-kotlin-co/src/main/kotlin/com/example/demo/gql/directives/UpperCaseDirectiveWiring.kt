package com.example.demo.gql.directives

import graphql.schema.DataFetcherFactories
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.idl.SchemaDirectiveWiring
import graphql.schema.idl.SchemaDirectiveWiringEnvironment
import java.util.*

class UpperCaseDirectiveWiring : SchemaDirectiveWiring {
    override fun onField(env: SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition>): GraphQLFieldDefinition {

        val field = env.element;
        val dataFetcher = DataFetcherFactories
            .wrapDataFetcher(
                env.fieldDataFetcher
            ) { _, value -> if (value is String && value.isNotEmpty()) value.uppercase(Locale.getDefault()) else value }

        env.fieldDataFetcher = dataFetcher;
        return field
    }
}
