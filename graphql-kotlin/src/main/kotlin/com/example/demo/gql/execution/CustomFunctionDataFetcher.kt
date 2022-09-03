package com.example.demo.gql.execution

import com.expediagroup.graphql.server.spring.execution.SpringDataFetcher
import graphql.schema.DataFetchingEnvironment
import org.springframework.context.ApplicationContext
import reactor.core.publisher.Mono
import kotlin.reflect.KFunction

class CustomFunctionDataFetcher(
    target: Any?,
    fn: KFunction<*>,
    appContext: ApplicationContext
) : SpringDataFetcher(target, fn, appContext) {

    override fun get(environment: DataFetchingEnvironment): Any? = when (val result = super.get(environment)) {
        is Mono<*> -> result.toFuture()
        else -> result
    }
}