package com.example.demo.gql;

import com.example.demo.service.AuthorNotFoundException;
import com.example.demo.service.PostNotFoundException;
import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomDataFetchingExceptionHandler implements DataFetcherExceptionHandler {
    private final SimpleDataFetcherExceptionHandler defaultHandler = new SimpleDataFetcherExceptionHandler();

    @Override
    public DataFetcherExceptionHandlerResult onException(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        if (exception instanceof AuthorNotFoundException || exception instanceof PostNotFoundException) {
            log.debug("caught exception: {}", exception);
            enum MyErrorType implements ErrorClassification {
                NOT_FOUND
            }
            GraphQLError graphqlError = GraphqlErrorBuilder.newError()
                    .message(exception.getMessage())
                    .errorType(MyErrorType.NOT_FOUND)
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
