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
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

// provide the real PostService in the slice, @MockitoSpyBean only wraps an existing bean
@GraphQlTest(controllers = {PostsDataFetcherController.class})
@Import(PostService.class)
class SubscriptionTests {

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    AuthorService authorService;

    @MockitoSpyBean
    PostService postService;

    @MockitoBean
    AuthorRepository authorRepository;

    @MockitoBean
    PostRepository postRepository;

    @MockitoBean
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
