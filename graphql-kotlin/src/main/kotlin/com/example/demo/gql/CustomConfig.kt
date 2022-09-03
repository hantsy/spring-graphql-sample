package com.example.demo.gql

import com.example.demo.gql.directives.UpperCaseDirectiveWiring
import com.example.demo.gql.exceptions.CustomDataFetcherExceptionHandler
import com.example.demo.gql.execution.CustomDataFetcherFactoryProvider
import com.example.demo.gql.execution.SpringDataFetcherFactory
import com.expediagroup.graphql.generator.directives.KotlinDirectiveWiringFactory
import com.expediagroup.graphql.generator.execution.FlowSubscriptionExecutionStrategy
import graphql.execution.DataFetcherExceptionHandler
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CustomConfig {

    // custom exception handling.
    @Bean
    fun dataFetcherExceptionHandler() = CustomDataFetcherExceptionHandler()

    @Bean
    fun dataFetcherFactoryProvider(
        springDataFetcherFactory: SpringDataFetcherFactory,
        applicationContext: ApplicationContext
    ) = CustomDataFetcherFactoryProvider(springDataFetcherFactory, applicationContext)

    @Bean
    fun directiveWiringFactory() = KotlinDirectiveWiringFactory(
        mapOf(UpperCaseDirectiveWiring.name to UpperCaseDirectiveWiring())
    )

    // to use Kotlin `Flow` in subscription
    @Bean
    fun schemaGeneratorHooks(wiringFactory: KotlinDirectiveWiringFactory) =
        CustomSchemaGeneratorHooks(wiringFactory)

    @Bean
    fun executionStrategy(dfe: DataFetcherExceptionHandler) = FlowSubscriptionExecutionStrategy(dfe)
}