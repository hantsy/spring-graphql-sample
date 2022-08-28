package com.example.demo.gql.exceptions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import graphql.ErrorType
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import graphql.execution.ResultPath
import graphql.language.SourceLocation
import graphql.ErrorType.ValidationError
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CustomDataFetcherExceptionHandler : DataFetcherExceptionHandler {
    companion object{
        private val log = LoggerFactory.getLogger(CustomDataFetcherExceptionHandler::class.java)
    }


    override fun onException(handlerParameters: DataFetcherExceptionHandlerParameters): DataFetcherExceptionHandlerResult {
        val exception = handlerParameters.exception
        val sourceLocation = handlerParameters.sourceLocation
        val path = handlerParameters.path

        val error: GraphQLError = when (exception) {
            is ValidationException -> ValidationDataFetchingGraphQLError(exception.constraintErrors, path, exception, sourceLocation)
            else ->
                GraphqlErrorException.newErrorException()
                    .cause(exception)
                    .message(exception.message)
                    .sourceLocation(sourceLocation)
                    .path(path.toList())
                    .build()
        }

        log.warn(error.message, exception)

        return DataFetcherExceptionHandlerResult.newResult().error(error).build()
    }
}

@JsonIgnoreProperties("exception")
class ValidationDataFetchingGraphQLError(
    val constraintErrors: List<ConstraintError>,
    path: ResultPath,
    exception: Throwable,
    sourceLocation: SourceLocation
) : ExceptionWhileDataFetching(
    path,
    exception,
    sourceLocation
) {
    override fun getErrorType(): ErrorType = ValidationError
}