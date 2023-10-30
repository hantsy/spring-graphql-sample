package com.example.demo;

import com.example.demo.gql.datafetchers.PostsDataFetcherController;
import com.example.demo.gql.types.CommentInput;
import com.example.demo.model.CommentEntity;
import com.example.demo.model.PostEntity;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

@GraphQlTest(controllers = {PostsDataFetcherController.class})
class SubscriptionTests {

    @Autowired
    GraphQlTester graphQlTester;

    @MockBean
    AuthorService authorService;

    @SpyBean
    PostService postService;

    @MockBean
    AuthorRepository authorRepository;

    @MockBean
    PostRepository postRepository;

    @MockBean
    CommentRepository commentRepository;

    @Test
    void createCommentAndSubscription() {
        String query = "subscription onCommentAdded { commentAdded { id postId content } }";
        Flux<String> subscribedCommentsFlux = graphQlTester.document(query)
                .executeSubscription()
                .toFlux("commentAdded.content", String.class);
        var verifier = StepVerifier.create(subscribedCommentsFlux)
                .expectNext("test comment")
                .thenCancel()
                .verifyLater();

        var postId = UUID.randomUUID();
        var commentId = UUID.randomUUID();
        when(postRepository.findById(any()))
                .thenReturn(
                        Mono.just(
                                new PostEntity(
                                        postId,
                                        "test title",
                                        "test content",
                                        "test status",
                                        LocalDateTime.now(),
                                        UUID.randomUUID()
                                )
                        )
                );
        when(commentRepository.create(any(), any())).thenReturn(Mono.just(commentId));
        when(commentRepository.findById(any()))
                .thenReturn(
                        Mono.just(
                                new CommentEntity(commentId, "test comment", LocalDateTime.now(), postId)
                        )
                );

        postService.addComment(new CommentInput("test comment", postId.toString()))
                .block(Duration.ofMillis(500));

        verifier.verify();

        // verify the invocation of the mocks.
        verify(postRepository, times(1)).findById(any());
        verify(commentRepository, times(1)).create(any(), any());
        verify(commentRepository, times(1)).findById(any());
        verify(postService, times(1)).commentAdded();
    }
}
