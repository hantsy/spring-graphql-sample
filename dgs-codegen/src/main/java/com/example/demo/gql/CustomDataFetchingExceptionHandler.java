package com.example.demo.gql;

import com.example.demo.service.AuthorNotFoundException;
import com.example.demo.service.PostNotFoundException;
import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler;
import com.netflix.graphql.types.errors.TypedGraphQLError;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomDataFetchingExceptionHandler implements DataFetcherExceptionHandler {
    private final DefaultDataFetcherExceptionHandler defaultHandler = new DefaultDataFetcherExceptionHandler();

    @Override
    public DataFetcherExceptionHandlerResult onException(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        if (exception instanceof AuthorNotFoundException || exception instanceof PostNotFoundException) {
            Map<String, Object> debugInfo = new HashMap<>();

            GraphQLError graphqlError = TypedGraphQLError.newNotFoundBuilder()
                    .message(exception.getMessage())
                    .debugInfo(debugInfo)
                    .path(handlerParameters.getPath())
                    .build();
            return DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build();
        } else {
            return defaultHandler.onException(handlerParameters);
        }
    }
}