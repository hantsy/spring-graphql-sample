package com.example.demo.gql;

import com.example.demo.service.AuthorNotFoundException;
import com.example.demo.service.PostNotFoundException;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class CustomDataFetchingExceptionHandler implements DataFetcherExceptionHandler {
    private final SimpleDataFetcherExceptionHandler defaultHandler = new SimpleDataFetcherExceptionHandler();

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        if (exception instanceof AuthorNotFoundException || exception instanceof PostNotFoundException) {

            GraphQLError graphqlError = GraphqlErrorBuilder.newError()
                    .message(exception.getMessage())
                    .errorType(ErrorType.DataFetchingException)
                    .path(handlerParameters.getPath())
                    .build();
            return CompletableFuture.completedFuture(DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build());
        } else {
            return defaultHandler.handleException(handlerParameters);
        }
    }
}