package com.example.demo;

import com.example.demo.gql.datafetcher.PostsDataFetcher;
import com.example.demo.gql.scalars.LocalDateTimeScalar;
import com.example.demo.gql.scalars.UUIDScalar;
import com.example.demo.gql.types.CommentInput;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.test.EnableDgsTest;
import graphql.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GraphQL execution behaviour of the `commentAdded` subscription, executed in-process through the
 * `DgsQueryExecutor` with the repositories mocked out.
 * See `IntegrationTests` for the transport level behaviour over SSE.
 */
@SpringBootTest(classes = SubscriptionTests.SubscriptionTestsConfig.class)
@EnableDgsTest
@Slf4j
class SubscriptionTests {

    @Configuration
    @Import({
            PostsDataFetcher.class,
            PostService.class,
            UUIDScalar.class,
            LocalDateTimeScalar.class
    })
    static class SubscriptionTestsConfig {

    }

    @Autowired
    DgsQueryExecutor dgsQueryExecutor;

    // The real service is used, so that the `Sinks` it publishes the comments with is fed the
    // same way as in production; only the repositories are mocked out.
    @MockitoSpyBean
    PostService postService;

    @MockitoBean
    PostRepository postRepository;

    @MockitoBean
    CommentRepository commentRepository;

    @MockitoBean
    AuthorRepository authorRepository;

    @Test
    @SuppressWarnings("unchecked")
    void commentAddedSubscription() {
        var comments = new CopyOnWriteArrayList<Map<String, Object>>();

        var subscriptionQuery = """
                subscription onCommentAdded {
                    commentAdded {
                        id
                        postId
                        content
                    }
                }
                """;

        ExecutionResult executionResult = dgsQueryExecutor.execute(subscriptionQuery, Map.of());
        Publisher<ExecutionResult> publisher = executionResult.getData();

        publisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(2);
            }

            @Override
            public void onNext(ExecutionResult result) {
                log.debug("execution result in publisher: {}", result);
                Map<String, Object> data = result.getData();
                comments.add((Map<String, Object>) data.get("commentAdded"));
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("subscription failed", throwable);
            }

            @Override
            public void onComplete() {
                log.debug("subscription completed");
            }
        });

        var postId = UUID.randomUUID();
        when(commentRepository.create(anyString(), eq(postId))).thenReturn(UUID.randomUUID());

        postService.addComment(
                CommentInput.newBuilder().postId(postId.toString()).content("Comment 1").build()
        );
        postService.addComment(
                CommentInput.newBuilder().postId(postId.toString()).content("Comment 2").build()
        );

        assertThat(comments).hasSize(2);
        assertThat(comments.stream().map(comment -> comment.get("content")).toList())
                .containsExactly("Comment 1", "Comment 2");

        // verify the invocation of the mocks.
        verify(commentRepository, times(2)).create(anyString(), eq(postId));
    }
}
