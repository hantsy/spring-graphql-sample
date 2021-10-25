package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import com.netflix.graphql.dgs.client.WebClientGraphQLClient;
import com.netflix.graphql.dgs.client.WebSocketGraphQLClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
@Disabled //see: https://github.com/Netflix/dgs-framework/issues/689
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

    @Test
    void createCommentAndSubscription() {
        var createPostQuery = "mutation createPost($input: CreatePostInput!){ createPost(createPostInput:$input) {id, title} }";
        var createPostVariables = Map.of(
                "input", Map.of(
                        "title", "test title",
                        "content", "test content"
                )
        );

        var createPostResult = this.client.reactiveExecuteQuery(createPostQuery, createPostVariables)
                .map(response -> response.extractValueAsObject("createPost", Post.class))
                .block(Duration.ofMillis(1000));

        log.debug("created post:{}", createPostResult);
        String postId = createPostResult.getId();
        assertThat(postId).isNotNull();

        String subscriptionQuery = "subscription onCommentAdded { commentAdded { id postId content } }";
        var executionResult = this.socketClient.reactiveExecuteQuery(subscriptionQuery, Collections.emptyMap())
                .map(it -> it.extractValueAsObject("data.commentAdded.content", String.class));

        var verifier = StepVerifier.create(executionResult)
                .consumeNextWith(it -> assertThat(it).isEqualTo("test comment"))
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

        this.client.reactiveExecuteQuery(addCommentQuery, addCommentVariables)
                .map(response -> response.extractValueAsObject("addComment", Comment.class))
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo("test comment"))
                .verifyComplete();

        verifier.verify();
    }
}
