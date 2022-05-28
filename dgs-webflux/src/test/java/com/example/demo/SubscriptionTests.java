package com.example.demo;

import com.example.demo.gql.datafetchers.PostsDataFetcher;
import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.CommentInput;
import com.example.demo.model.CommentEntity;
import com.example.demo.model.PostEntity;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor;
import com.netflix.graphql.dgs.webflux.autoconfiguration.DgsWebFluxAutoConfiguration;
import graphql.ExecutionResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = SubscriptionTests.SubscriptionTestsConfig.class)
@Slf4j
//@Disabled
// see: https://github.com/Netflix/dgs-framework/discussions/605
class SubscriptionTests {

    @Configuration
    @Import({PostsDataFetcher.class, PostService.class})
    @ImportAutoConfiguration(classes = {
            DgsWebFluxAutoConfiguration.class,
            WebFluxAutoConfiguration.class,
            DgsAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    static class SubscriptionTestsConfig {

    }

    @Autowired
    DgsReactiveQueryExecutor dgsReactiveQueryExecutor;

    @Autowired
    ObjectMapper objectMapper;

    @SpyBean
    PostService postService;

    @MockBean
    PostRepository postRepository;

    @MockBean
    CommentRepository commentRepository;

    @MockBean
    AuthorRepository authorRepository;

    @MockBean
    AuthorService authorService;

    @SneakyThrows
    @Test
    void createCommentAndSubscription() {
        when(postRepository.findById(anyLong())).thenReturn(
                Mono.just(
                        new PostEntity(1L, "test", "test content", "DRAFT", 2L)
                )
        );
        when(commentRepository.create(anyString(), anyLong())).thenReturn(Mono.just(2L));
        when(commentRepository.findById(anyLong())).thenReturn(
                Mono.just(
                        new CommentEntity(2L, "test comment", 1L)
                )
        );

        // commentAdded producer
        var comments = new CopyOnWriteArrayList<Comment>();

        @Language("GraphQL") var subscriptionQuery = "subscription onCommentAdded { commentAdded { id postId content } }";
//        var executionResult = dgsReactiveQueryExecutor.execute(subscriptionQuery, Collections.emptyMap()).block();
//        var publisher = executionResult.<Publisher<ExecutionResult>>getData();
//        publisher.subscribe(new Subscriber<ExecutionResult>() {
//            @Override
//            public void onSubscribe(Subscription s) {
//                s.request(2);
//            }
//
//            @Override
//            public void onNext(ExecutionResult executionResult) {
//                log.debug("execution result in publisher: {}", executionResult);
//                var commentAdded = objectMapper.convertValue(
//                        executionResult.<Map<String, Object>>getData().get("commentAdded"),
//                        Comment.class
//                );
//                log.debug("commentAdded event: {}", commentAdded);
//                comments.add(commentAdded);
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                log.debug("error:" + t);
//            }
//
//            @Override
//            public void onComplete() {
//                log.debug(">>>completed...");
//            }
//        });
//
        var executionResultMono = dgsReactiveQueryExecutor.execute(subscriptionQuery, Collections.emptyMap());
        var publisher = executionResultMono.flatMapMany(result -> result.<Publisher<ExecutionResult>>getData());
        publisher.subscribe(executionResult -> {
            log.debug("execution result in publisher: {}", executionResult);
            var commentAdded = objectMapper.convertValue(
                    executionResult.<Map<String, Object>>getData().get("commentAdded"),
                    Comment.class
            );
            log.debug("commentAdded event: {}", commentAdded);
            comments.add(commentAdded);
        });

        var countDownLatch = new CountDownLatch(1);//make sure the `add comment` is done successfully.
        postService.addComment(CommentInput.builder().postId(1l).content("test comment").build())
                .then(
                        postService.addComment(CommentInput.builder().postId(1l).content("test comment").build())
                )
                .doOnTerminate(countDownLatch::countDown)
                .subscribe(data -> log.debug("added comment:{}", data));
        countDownLatch.await(5, SECONDS);

        // verify
        await()
                .atMost(5, SECONDS)
                .untilAsserted(
                        () -> assertThat(comments.size()).isEqualTo(2)
                );

        // verify the invocation of the mocks.
        verify(postRepository, times(2)).findById(anyLong());
        verify(commentRepository, times(2)).create(anyString(), anyLong());
        verify(commentRepository, times(2)).findById(anyLong());
    }
}
