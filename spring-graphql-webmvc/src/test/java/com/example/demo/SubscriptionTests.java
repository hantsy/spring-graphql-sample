package com.example.demo;

import com.example.demo.gql.PostController;
import com.example.demo.gql.types.CommentInput;
import com.example.demo.model.CommentEntity;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

@GraphQlTest(controllers = {PostController.class})
@Slf4j
public class SubscriptionTests {

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

    @SneakyThrows
    @Test
    public void createPostAndAddComment() {
        String query = "subscription onCommentAdded { commentAdded { id postId content } }";
        val subscribedCommentsFlux = graphQlTester.document(query)
                .executeSubscription()
                .toFlux("commentAdded.content", String.class);

        val verifier = StepVerifier.create(subscribedCommentsFlux)
                .expectNext("test comment")
                .thenCancel()
                .verifyLater();

        var postId = UUID.randomUUID();
        var commentId = UUID.randomUUID();
        when(commentRepository.create(any(), any())).thenReturn(commentId);
        when(commentRepository.findById(any())).thenReturn(
                new CommentEntity(
                        commentId,
                        "test comment",
                        LocalDateTime.now(),
                        postId)
        );

        // add a comment will raise commentAdded event.
        postService.addComment(
                CommentInput.builder()
                        .postId(postId.toString())
                        .content("test content")
                        .build()
        );

        Awaitility.await().atMost(Duration.ofMillis(500)).untilAsserted(
                () -> verifier.verifyThenAssertThat().hasNotDiscardedElements()
        );

        // verify the invocation of the mocks.
        verify(commentRepository, times(1)).create(any(), any());
        verify(commentRepository, times(1)).findById(any());
        verify(postService, times(1)).commentAdded();
    }

}
