package com.example.demo;

import com.example.demo.gql.client.CreatePostGraphQLQuery;
import com.example.demo.gql.types.CreatePostInput;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MutationTests {

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @Test
    void testValidateCreatePost() {
        var queryRequest = new GraphQLQueryRequest(
                CreatePostGraphQLQuery.newRequest()
                        .createPostInput(
                                CreatePostInput.newBuilder()
                                        .title("test")// size is invalid
                                        .content("content of test")
                                        .build()
                        )
                        .build()
        );
        var result = dgsQueryExecutor.execute(queryRequest.serialize());
        assertThat(result.getErrors()).isNotEmpty();
    }

}
