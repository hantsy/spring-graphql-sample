package com.example.demo.gql

import com.example.demo.AuthorNotFoundException
import com.example.demo.PostNotFoundException
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class ExceptionHandlers : DataFetcherExceptionResolverAdapter() {
    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
        return when (ex) {
            is PostNotFoundException, is AuthorNotFoundException -> GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.NOT_FOUND)
                .message(ex.message)
                .build()
            else -> super.resolveToSingleError(ex, env)
        }
    }
}