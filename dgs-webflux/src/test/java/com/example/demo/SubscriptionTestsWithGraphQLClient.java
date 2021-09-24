package com.example.demo;

import com.example.demo.gql.types.Comment;
import com.example.demo.gql.types.Post;
import com.netflix.graphql.dgs.client.DefaultGraphQLClient;
import com.netflix.graphql.dgs.client.HttpResponse;
import com.netflix.graphql.dgs.client.MonoRequestExecutor;
import com.netflix.graphql.dgs.reactive.DgsReactiveQueryExecutor;
import graphql.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
@Disabled //see: https://github.com/Netflix/dgs-framework/discussions/605
class SubscriptionTestsWithGraphQLClient {

    @Autowired
    DgsReactiveQueryExecutor dgsReactiveQueryExecutor;

    @LocalServerPort
    int port;

    WebClient webClient;

    DefaultGraphQLClient client;

    @BeforeEach
    public void setup() {
        this.webClient = WebClient.create("http://localhost:" + port);
        this.client = new DefaultGraphQLClient("http://localhost:" + port);
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
        MonoRequestExecutor monoRequestExecutor = (url, headers, body) -> this.webClient.post().uri("/graphql")
                .headers(it -> it.putAll(headers))
                .bodyValue(body)
                .retrieve()
                .toEntity(String.class)
                .map(entity -> new HttpResponse(entity.getStatusCodeValue(), entity.getBody()));

        var createPostResult = this.client.reactiveExecuteQuery(createPostQuery, createPostVariables, monoRequestExecutor)
                .map(response -> response.extractValueAsObject("createPost", Post.class))
                .block(Duration.ofMillis(1000));

        log.debug("created post:{}", createPostResult);
        String postId = createPostResult.getId();
        assertThat(postId).isNotNull();

        String query = "subscription onCommentAdded { commentAdded { id postId content } }";
        var executionResultMono = dgsReactiveQueryExecutor.execute(query, Collections.emptyMap());
        var result = executionResultMono.flatMapMany(it -> Flux.from(it.<Publisher<ExecutionResult>>getData()));

        var verifier = StepVerifier.create(result)
                .consumeNextWith(it -> {
                    log.debug("publisher: {}", it);
                    var data = it.<Map<String, Map<String, Object>>>getData();
                    log.debug("data: {}", data);
                    assertThat(data.get("commentAdded").get("content")).isEqualTo("test comment");
                })
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

        this.client.reactiveExecuteQuery(addCommentQuery, addCommentVariables, monoRequestExecutor)
                .map(response -> response.extractValueAsObject("addComment", Comment.class))
                .as(StepVerifier::create)
                .consumeNextWith(comment -> assertThat(comment.getContent()).isEqualTo("test comment"))
                .verifyComplete();

        verifier.verify();
    }
}
