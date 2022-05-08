package com.example.demo.gql;

import com.example.demo.service.AuthorNotFoundException;
import com.example.demo.service.PostNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class ExceptionHandlers implements DataFetcherExceptionResolver {
    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment environment) {
        if (exception instanceof PostNotFoundException || exception instanceof AuthorNotFoundException) {
            return Mono.fromCallable(() -> Arrays.asList(
                    GraphqlErrorBuilder.newError(environment)
                            .errorType(ErrorType.NOT_FOUND)
                            .message(exception.getMessage())
                            .build()));
        }
        return Mono.empty();
    }
}
