package com.example.demo;

import com.example.demo.gql.datafetchers.PostsDataFetcher;
import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.CommentInput;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.boot.test.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@GraphQlTest
class SubscriptionTests {

    @Autowired
    GraphQlTester graphQlTester;

    @MockBean
    PostService postService;

    @MockBean
    AuthorService authorService;

    @Autowired
    PostsDataFetcher dataFetcher;

    @Test
    void createCommentAndSubscription() {
        when(postService.addComment(any(CommentInput.class))).thenReturn(Mono.just(UUID.randomUUID()));
        when(postService.getCommentById(anyString())).thenReturn(Mono.just(
                Comment.builder().id(UUID.randomUUID().toString())
                        .content("test comment")
                        .postId(UUID.randomUUID().toString())
                        .build()
        ));

        String query = "subscription onCommentAdded { commentAdded { id postId content } }";
        var verifier = graphQlTester.query(query)
                .executeSubscription()
                .toFlux("commentAdded.content", String.class)
                .as(StepVerifier::create)
                .expectNext("test comment")
                .thenCancel()
                .verifyLater();

        // add comment
        dataFetcher.addComment(
                        CommentInput.builder()
                                .postId(UUID.randomUUID().toString())
                                .content("test content")
                                .build()
                )
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo("test comment"))
                .verifyComplete();

        // verify the subscription now.
        verifier.verify();

        // verify the invocation of the mocks.
        verify(postService, times(1)).addComment(any(CommentInput.class));
        verify(postService, times(1)).getCommentById(anyString());
        verifyNoMoreInteractions(postService);
    }
}
