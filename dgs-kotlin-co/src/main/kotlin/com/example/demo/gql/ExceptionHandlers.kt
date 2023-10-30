package com.example.demo.gql

import com.example.demo.service.AuthorNotFoundException
import com.example.demo.service.PostNotFoundException
import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler
import com.netflix.graphql.types.errors.TypedGraphQLError
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class ExceptionHandlers : DataFetcherExceptionHandler {
    val defaultHandler = DefaultDataFetcherExceptionHandler()

    override fun handleException(handlerParameters: DataFetcherExceptionHandlerParameters): CompletableFuture<DataFetcherExceptionHandlerResult>? =
        when (val exception = handlerParameters.exception) {
            is PostNotFoundException, is AuthorNotFoundException -> {
                val graphqlError = TypedGraphQLError.newNotFoundBuilder()
                    .message(exception.message)
                    .path(handlerParameters.path)
                    .build();
                CompletableFuture.completedFuture(
                    DataFetcherExceptionHandlerResult.newResult()
                        .error(graphqlError)
                        .build()
                )
            }

            else -> {
                defaultHandler.handleException(handlerParameters)
            }
        }
}