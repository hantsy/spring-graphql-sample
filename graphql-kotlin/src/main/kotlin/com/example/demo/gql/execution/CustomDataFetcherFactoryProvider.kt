package com.example.demo.gql.execution

import com.expediagroup.graphql.generator.execution.SimpleKotlinDataFetcherFactoryProvider
import graphql.schema.DataFetcherFactory
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
/**
 * Custom DataFetcherFactory provider that returns custom Spring based DataFetcherFactory for resolving lateinit properties.
 */
class CustomDataFetcherFactoryProvider(
    private val springDataFetcherFactory: SpringDataFetcherFactory,
    private val applicationContext: ApplicationContext
) : SimpleKotlinDataFetcherFactoryProvider() {

    override fun functionDataFetcherFactory(target: Any?, kFunction: KFunction<*>) = DataFetcherFactory {
        CustomFunctionDataFetcher(
            target = target,
            fn = kFunction,
            appContext = applicationContext
        )
    }

    override fun propertyDataFetcherFactory(kClass: KClass<*>, kProperty: KProperty<*>): DataFetcherFactory<Any?> =
        if (kProperty.isLateinit) {
            springDataFetcherFactory
        } else {
            super.propertyDataFetcherFactory(kClass, kProperty)
        }
}