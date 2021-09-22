package com.example.demo;

import com.example.demo.gql.client.CreatePostGraphQLQuery;
import com.example.demo.gql.types.CreatePostInput;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class MutationTests {

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    @MockBean
    PostService postService;

    @Test
    void testCreatePost() {
        UUID id = UUID.randomUUID();
        when(postService.createPost(any(CreatePostInput.class))).thenReturn(id);
        var queryRequest = new GraphQLQueryRequest(
                CreatePostGraphQLQuery.newRequest()
                        .createPostInput(
                                CreatePostInput.newBuilder()
                                        .title("test title")
                                        .content("content of test")
                                        .build()
                        )
                        .build()
        );
        String result = dgsQueryExecutor.executeAndExtractJsonPath(queryRequest.serialize(), "data.createPost");
        assertThat(result).isEqualTo(id.toString());
        verify(postService,times(1)).createPost(any(CreatePostInput.class));
        verifyNoMoreInteractions(postService);
    }

    @Test
    void testValidateCreatePost() {
        when(postService.createPost(any(CreatePostInput.class))).thenReturn(UUID.randomUUID());
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
        verifyNoInteractions(postService);
    }

}
