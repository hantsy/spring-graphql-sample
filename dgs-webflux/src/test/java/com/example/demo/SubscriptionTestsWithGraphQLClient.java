package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import com.netflix.graphql.dgs.client.WebClientGraphQLClient;
import com.netflix.graphql.dgs.client.WebSocketGraphQLClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
//@Disabled
//see: https://github.com/Netflix/dgs-framework/issues/689
class SubscriptionTestsWithGraphQLClient {

    @LocalServerPort
    int port;

    private WebClientGraphQLClient client;

    private WebSocketGraphQLClient socketClient;

    @BeforeEach
    public void setup() {
        var webClient = WebClient.create("http://localhost:" + port + "/graphql");
        this.client = new WebClientGraphQLClient(webClient);
        this.socketClient = new WebSocketGraphQLClient("ws://localhost:" + port + "/subscriptions", new ReactorNettyWebSocketClient());
    }

    @SneakyThrows
    @Test
    void createCommentAndSubscription() {
        var createPostQuery = "mutation createPost($input: CreatePostInput!){ createPost(createPostInput:$input) {id, title} }";
        var createPostVariables = Map.of(
                "input", Map.of(
                        "title", "test title",
                        "content", "test content"
                )
        );

        var countDownLatch = new CountDownLatch(1);
        var postIdHolder = new PostIdHolder();
        var createPostResult = this.client.reactiveExecuteQuery(createPostQuery, createPostVariables)
                .map(response -> response.extractValueAsObject("createPost", Post.class))
                .map(Post::getId)
                .doOnTerminate(countDownLatch::countDown)
                .subscribe(id -> {
                    log.debug("post created, id: {}", id);
                    postIdHolder.setPostId(id);
                });
        countDownLatch.await(5, SECONDS);

        log.debug("created post:{}", createPostResult);
        Long postId = postIdHolder.getPostId();
        log.debug("post id get from amotic long: {}", postId);
        assertThat(postId).isNotNull();

        String subscriptionQuery = "subscription onCommentAdded { commentAdded { id postId content } }";
        var executionResultMono = this.socketClient
                .reactiveExecuteQuery(subscriptionQuery, Collections.emptyMap());
        var publisher = executionResultMono
                .map(it -> it.extractValueAsObject("commentAdded", Comment.class));
        var verifier = StepVerifier.create(publisher)
                .expectNextCount(1)
                .thenCancel()
                .verifyLater();

        // add comment
        var addCommentQuery = "mutation addComment($input: CommentInput!) { addComment(commentInput:$input) { id postId content}}";
        var addCommentVariables = Map.of(
                "input", Map.of(
                        "postId", postId,
                        "content", "test comment"
                )
        );

        var addCommentVariables2 = Map.of(
                "input", Map.of(
                        "postId", postId,
                        "content", "test comment2"
                )
        );

        this.client.reactiveExecuteQuery(addCommentQuery, addCommentVariables)
                .map(response -> response.extractValueAsObject("addComment", Comment.class))
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo("test comment"))
                .verifyComplete();

        this.client.reactiveExecuteQuery(addCommentQuery, addCommentVariables2)
                .map(response -> response.extractValueAsObject("addComment", Comment.class))
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo("test comment2"))
                .verifyComplete();

        // verify
        await()
                .atMost(5, SECONDS)
                .untilAsserted(
                        () -> verifier.verify()
                );

    }
}

class PostIdHolder {
    private Long postId;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }
}