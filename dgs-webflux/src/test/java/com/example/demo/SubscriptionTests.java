package com.example.demo;

import com.example.demo.gql.datafetchers.PostsDataFetcher;
import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.CommentInput;
import com.example.demo.service.AuthorService;
import com.example.demo.service.PostService;
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor;
import graphql.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest()
@Slf4j
@Disabled//see: https://github.com/Netflix/dgs-framework/discussions/605
class SubscriptionTests {

    @Autowired
    DgsReactiveQueryExecutor dgsReactiveQueryExecutor;

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

        String query = "subscription onCommentAdded { commentAdded { id postId content } }";
        Mono<ExecutionResult> executionResultMono = dgsReactiveQueryExecutor.execute(query, Collections.emptyMap());

        var publisher = executionResultMono.flatMapMany(result -> Flux.from( result.<Publisher<ExecutionResult>>getData()));

        var verifier = StepVerifier.create(publisher)
                .consumeNextWith(it -> {
                    log.debug("publisher: {}", it);
                    var data = it.<Map<String, Map<String, Object>>>getData();
                    log.debug("data: {}", data);
                    assertThat(data.get("commentAdded").get("content")).isEqualTo("test comment");
                })
                .thenCancel()
                .verifyLater();



        verifier.verify();
        verify(postService, times(1)).addComment(any(CommentInput.class));
        verify(postService, times(1)).getCommentById(anyString());
        verifyNoMoreInteractions(postService);
    }
}
