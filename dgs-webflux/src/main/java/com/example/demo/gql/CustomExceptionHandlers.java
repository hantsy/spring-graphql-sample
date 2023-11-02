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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class CustomExceptionHandlers implements DataFetcherExceptionHandler {
    private final DefaultDataFetcherExceptionHandler defaultHandler = new DefaultDataFetcherExceptionHandler();

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        if (exception instanceof PostNotFoundException || exception instanceof AuthorNotFoundException) {
            Map<String, Object> debugInfo = new HashMap<>();

            GraphQLError graphqlError = TypedGraphQLError.newNotFoundBuilder()
                    .message(exception.getMessage())
                    .debugInfo(debugInfo)
                    .path(handlerParameters.getPath())
                    .build();
            return CompletableFuture.completedFuture(DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build());
        } else if (exception instanceof ConstraintViolationException ex) {
            Map<String, Object> debugInfo = new HashMap<>();

            Map<String, Object> extensions = new HashMap<>();
            for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
                String path = violation.getPropertyPath().toString();
                String message = violation.getMessage();
                extensions.put(path, message);
            }

            GraphQLError graphqlError = TypedGraphQLError.newBadRequestBuilder()
                    .message("validation failed")
                    .debugInfo(debugInfo)
                    .path(handlerParameters.getPath())
                    .extensions(extensions)
                    .build();
            return CompletableFuture.completedFuture(DataFetcherExceptionHandlerResult.newResult()
                    .error(graphqlError)
                    .build());
        } else {
            return defaultHandler.handleException(handlerParameters);
        }
    }
}
