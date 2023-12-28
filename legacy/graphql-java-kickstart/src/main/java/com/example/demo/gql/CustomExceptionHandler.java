package com.example.demo.gql;

import com.example.demo.service.AuthorNotFoundException;
import com.example.demo.service.PostNotFoundException;
import graphql.kickstart.spring.error.ErrorContext;
import graphql.kickstart.spring.error.ThrowableGraphQLError;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Component
public class CustomExceptionHandler {

    @ExceptionHandler({AuthorNotFoundException.class, PostNotFoundException.class})
    public ThrowableGraphQLError onException(Exception e, ErrorContext context) {
        return new ThrowableGraphQLError(e, HttpStatus.NOT_FOUND.getReasonPhrase());
    }
}