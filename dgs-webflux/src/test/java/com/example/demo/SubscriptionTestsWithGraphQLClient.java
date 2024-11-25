package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import com.netflix.graphql.dgs.client.WebClientGraphQLClient;
import com.netflix.graphql.dgs.client.WebSocketGraphQLClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

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
        @Language("graphql") var createPostQuery = """
                mutation createPost($input: CreatePostInput!){
                    createPost(createPostInput:$input) {
                        id
                        title
                    }
                }
                """.stripIndent();
        var createPostVariables = Map.of(
                "input", Map.of(
                        "title", "test title",
                        "content", "test content"
                )
        );

        var countDownLatch = new CountDownLatch(1);
        var postIdHolder = new AtomicLong();
        var createPostResult = this.client.reactiveExecuteQuery(createPostQuery, createPostVariables)
                .map(response -> response.extractValueAsObject("createPost", Post.class))
                .map(Post::getId)
                //.doOnTerminate(countDownLatch::countDown)
                .subscribe(id -> {
                    log.debug("post created, id: {}", id);
                    postIdHolder.set(id);
                    countDownLatch.countDown();
                });
        countDownLatch.await(5, SECONDS);

        log.debug("created post:{}", createPostResult);
        Long postId = postIdHolder.get();
        log.debug("post id get from amotic long: {}", postId);
        assertThat(postId).isNotNull();

        @Language("graphql") var subscriptionQuery = """
                subscription onCommentAdded {
                    commentAdded {
                        id
                        postId
                        content
                    }
                }
                """.stripIndent();
        var executionResultPublisher = this.socketClient
                .reactiveExecuteQuery(subscriptionQuery, Collections.emptyMap());
        var commentAddedDataPublisher = executionResultPublisher
                .map(it -> it.extractValueAsObject("commentAdded", Comment.class));

        // add two comments
        String comment1 = "test comment";
        String comment2 = "test comment2";
        var verifier = StepVerifier.create(commentAddedDataPublisher)
                .thenAwait(Duration.ofMillis(1000)) // see: https://github.com/Netflix/dgs-framework/issues/657
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo(comment1))
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo(comment2))
                .thenCancel()
                .verifyLater();

        // add comment
        @Language("graphql") var addCommentQuery = """
                mutation addComment($input: CommentInput!) {
                    addComment(commentInput:$input) {
                        id
                        postId
                        content
                    }
                }
                """.stripIndent();

        var addCommentVariables = Map.of(
                "input", Map.of(
                        "postId", postId,
                        "content", comment1
                )
        );

        var addCommentVariables2 = Map.of(
                "input", Map.of(
                        "postId", postId,
                        "content", comment2
                )
        );

        this.client.reactiveExecuteQuery(addCommentQuery, addCommentVariables)
                .map(response -> response.extractValueAsObject("addComment", Comment.class))
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo(comment1))
                .verifyComplete();

        this.client.reactiveExecuteQuery(addCommentQuery, addCommentVariables2)
                .map(response -> response.extractValueAsObject("addComment", Comment.class))
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo(comment2))
                .verifyComplete();

        // verify
        // await().atMost(5, SECONDS).untilAsserted(verifier::verify);
        verifier.verify();
    }
}
