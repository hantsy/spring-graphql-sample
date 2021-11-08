package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
//@Disabled
class IntegrationTests {

    WebSocketClient client;

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    String url;

    @BeforeEach
    void setUp() {
        this.client = new ReactorNettyWebSocketClient();
        this.url = "ws://localhost:" + port + "/ws/graphql";
        log.debug("websocket connection url: {}", this.url);
    }

    @Test
    void createPostAndAddCommentAndSubscription() {
        //create post
        var createPostQuery = """
                mutation createPost($input: CreatePostInput!){
                    createPost(createPostInput:$input){
                        id
                        title
                        content
                    }
                }""".trim();
        var createPostOperationData = Map.of(
                "payload", Map.of(
                        "query", createPostQuery,
                        "variables", Map.of(
                                "input", Map.of(
                                        "title", "test title",
                                        "content", "test content"
                                )
                        )
                ),
                "type", "subscribe",
                "id", "1"
        );
        var connectionInitData = Map.<String, Object>of(
                "type", "connection_init",
                "id", "1"
        );
        final String inputDataJson = toJson(createPostOperationData);
        log.debug("input data json string: {}", inputDataJson);
        final String connectionInitDataJson = toJson(connectionInitData);
        log.debug("connection init json string: {}", connectionInitDataJson);

        var titleReplay = new ArrayList<String>();
        WebSocketHandler createPostHandler = session -> {
            var receiveMono = session.receive().log("receive::")
                    .doOnNext(webSocketMessage -> {
                        var text = webSocketMessage.getPayloadAsText();
                        log.debug("websocket message: {}", text);
                        String type = JsonPath.read(text, "type");
                        if ("connection_ack".equals(type)) {//do nothing
                            return ;
                        } else if ("next".equals(type)) {
                            String title = JsonPath.read(text, "payload.data.createPost.title");
                            titleReplay.add(title);
                            assertThat(title).isEqualTo("test title");
                            String postId = JsonPath.read(text, "payload.data.createPost.id");
                            log.debug("title: {}, id: {}", title, postId);
                        }
                    })
                    .doOnError(error -> log.debug("error on receiving:" + error))
                    .doOnComplete(() -> log.debug("called doOnComplete() on receiving"))
                    .then();

            var sendMono = session.send(
                    Mono.delay(Duration.ofMillis(500)).thenMany(
                            Flux.just(connectionInitDataJson, inputDataJson)
                                    .map(session::textMessage)
                    ).log("send::")
            );
            return sendMono.then(receiveMono);
        };
        this.client.execute(URI.create(this.url), createPostHandler).block(Duration.ofMillis(5000));
        //verify the message.
        assertThat(titleReplay.size()).isEqualTo(1);
        assertThat(titleReplay.get(0)).isEqualTo("test title");

//        var postId = graphQlTester.query(createPostQuery)
//                .variable("input", Map.of(
//                        "title", "test title",
//                        "content", "test content"
//                ))
//                .execute()
//                .path("data.createPost.title").entity(String.class).isEqualTo("test title")
//                .path("data.createPost.id").entity(String.class).get();
//        log.debug("saved post id: {}", postId);
//        assertThat(postId).isNotNull();
//
//        // subscribe commentAdded event
//        String subscriptionQuery = "subscription onCommentAdded { commentAdded { id postId content } }";
//        var verifier = graphQlTester.query(subscriptionQuery)
//                .executeSubscription()
//                .toFlux("commentAdded.content", String.class)
//                .as(StepVerifier::create)
//                .expectNext("test comment")
//                .thenCancel()
//                .verifyLater();
//
//        // add comment
//        var addCommentQuery = "mutation addComment($input: CommentInput!){addComment(commentInput:$input){id, content}}";
//        graphQlTester.query(createPostQuery)
//                .variable("input", Map.of(
//                        "postId", postId,
//                        "content", "test comment"
//                ))
//                .execute()
//                .path("data.addComment.content").entity(String.class).isEqualTo("test comment");
//
//        // verify the subscription now.
//        verifier.verify();
    }

    @SneakyThrows
    private String toJson(Map<String, Object> createPostOperationData) {
        return objectMapper.writeValueAsString(createPostOperationData);
    }
}
