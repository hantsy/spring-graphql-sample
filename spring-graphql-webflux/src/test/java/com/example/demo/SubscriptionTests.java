package com.example.demo;

import com.example.demo.gql.datafetchers.PostsDataFetcherController;
import com.example.demo.gql.types.Comment;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.*;

@GraphQlTest(controllers = {PostsDataFetcherController.class})
class SubscriptionTests {

    @Autowired
    GraphQlTester graphQlTester;

    @MockBean
    PostService postService;

    @MockBean
    AuthorService authorService;

    @Test
    void createCommentAndSubscription() {
        when(postService.commentAdded()).thenReturn(Flux.just(
                Comment.builder().id(UUID.randomUUID().toString())
                        .content("test comment")
                        .postId(UUID.randomUUID().toString())
                        .build()
        ));

        String query = "subscription onCommentAdded { commentAdded { id postId content } }";
        var verifier = graphQlTester.document(query)
                .executeSubscription()
                .toFlux("commentAdded.content", String.class)
                .as(StepVerifier::create)
                .expectNext("test comment")
                .thenCancel()
                .verify();

        // verify the invocation of the mocks.
        verify(postService, times(1)).commentAdded();
        verifyNoMoreInteractions(postService);
    }
}
